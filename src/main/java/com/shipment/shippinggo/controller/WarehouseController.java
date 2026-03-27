package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.WarehouseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/warehouse")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final BusinessDayService businessDayService;
    private final OrganizationService organizationService;
    private final OrderService orderService;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public WarehouseController(WarehouseService warehouseService,
            BusinessDayService businessDayService,
            OrganizationService organizationService,
            OrderService orderService,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.warehouseService = warehouseService;
        this.businessDayService = businessDayService;
        this.organizationService = organizationService;
        this.orderService = orderService;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    /**
     * قائمة أيام المخزن (نفس أيام العمل)
     */
    @GetMapping
    public String listWarehouseDays(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {

        model.addAttribute("businessDays", businessDayService.getBusinessDaysForUser(org.getId(), user));
        model.addAttribute("organization", org);
        return "warehouse/list";
    }

    /**
     * عرض المخزن الأساسي ليوم عمل (أوردرات WAITING + المرتجعات المؤكدة)
     */
    @GetMapping("/{id}")
    public String viewWarehouseDay(@PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long incomingFromId,
            @RequestParam(required = false) Long outgoingToId,
            @RequestParam(required = false) OrderStatus status,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        BusinessDay businessDay = businessDayService.getById(id);
        if (businessDay == null) {
            return "redirect:/warehouse";
        }

        // التحقق أن يوم العمل يتبع لمنظمة المستخدم
        if (!businessDay.getOrganization().getId().equals(org.getId())) {
            return "redirect:/warehouse";
        }

        // جلب الأوردرات بالفلاتر كاملة من الداتابيز
        List<Order> orders = warehouseService.getWarehouseOrders(id, search, code, courierId, null, status,
                incomingFromId, outgoingToId);

        model.addAttribute("businessDay", businessDay);
        model.addAttribute("organization", org);
        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("code", code);
        model.addAttribute("courierId", courierId);
        model.addAttribute("incomingFromId", incomingFromId);
        model.addAttribute("outgoingToId", outgoingToId);
        model.addAttribute("status", status);

        // بيانات الفلاتر
        model.addAttribute("couriers", organizationService.getCouriers(org));
        model.addAttribute("linkedOrganizations", organizationService.getLinkedOrganizations(org));
        model.addAttribute("statuses", OrderStatus.values());

        // حساب سياق سلسلة الإسناد (للعرض فقط)
        java.util.Map<Long, java.util.Map<String, String>> chainCtx = orderService.getOrderChainContext(orders,
                org.getId());
        model.addAttribute("orderChainContext", chainCtx);

        java.util.Map<Long, java.util.Map<String, Boolean>> returnCtx = orderService.getOrderReturnContext(orders,
                org.getId());
        model.addAttribute("returnContext", returnCtx);

        // حساب الإحصائيات في دورة واحدة
        long waitingCount = 0, cancelledCount = 0, refusedCount = 0, deferredCount = 0, awaitingReceiptCount = 0;
        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.WAITING)
                waitingCount++;
            else if (o.getStatus() == OrderStatus.CANCELLED)
                cancelledCount++;
            else if (o.getStatus() == OrderStatus.REFUSED)
                refusedCount++;
            else if (o.getStatus() == OrderStatus.DEFERRED)
                deferredCount++;

            if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REFUSED
                    || o.getStatus() == OrderStatus.DEFERRED
                    || o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                java.util.Map<String, Boolean> ctx = returnCtx.get(o.getId());
                if (ctx == null || !ctx.getOrDefault("hasConfirmed", false)) {
                    awaitingReceiptCount++;
                }
            }
        }

        model.addAttribute("totalOrderCount", orders.size());
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("refusedCount", refusedCount);
        model.addAttribute("deferredCount", deferredCount);

        model.addAttribute("awaitingReceiptCount", awaitingReceiptCount);

        return "warehouse/day";
    }

    /**
     * عرض المخزن الخارجي (IN_TRANSIT + في انتظار تأكيد الاستلام)
     */
    @GetMapping("/{id}/external")
    public String viewExternalWarehouse(@PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long incomingFromId,
            @RequestParam(required = false) Long outgoingToId,
            @RequestParam(required = false) OrderStatus status,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        BusinessDay businessDay = businessDayService.getById(id);
        if (businessDay == null) {
            return "redirect:/warehouse";
        }

        if (!businessDay.getOrganization().getId().equals(org.getId())) {
            return "redirect:/warehouse";
        }

        // جلب الأوردرات بالفلاتر كاملة من الداتابيز
        List<Order> orders = warehouseService.getExternalWarehouseOrders(id, search, code, courierId, null, status,
                incomingFromId, outgoingToId);

        model.addAttribute("businessDay", businessDay);
        model.addAttribute("organization", org);
        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("code", code);
        model.addAttribute("courierId", courierId);
        model.addAttribute("incomingFromId", incomingFromId);
        model.addAttribute("outgoingToId", outgoingToId);
        model.addAttribute("status", status);

        // بيانات الفلاتر
        model.addAttribute("couriers", organizationService.getCouriers(org));
        model.addAttribute("linkedOrganizations", organizationService.getLinkedOrganizations(org));
        model.addAttribute("statuses", OrderStatus.values());

        // حساب سياق سلسلة الإسناد (للعرض فقط)
        java.util.Map<Long, java.util.Map<String, String>> chainCtx = orderService.getOrderChainContext(orders,
                org.getId());
        model.addAttribute("orderChainContext", chainCtx);

        java.util.Map<Long, java.util.Map<String, Boolean>> returnCtx = orderService.getOrderReturnContext(orders,
                org.getId());
        model.addAttribute("returnContext", returnCtx);

        // حساب الإحصائيات في دورة واحدة
        long inTransitCount = 0, deliveredCount = 0, cancelledCount = 0, refusedCount = 0, deferredCount = 0,
                awaitingReceiptCount = 0;
        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.IN_TRANSIT)
                inTransitCount++;
            else if (o.getStatus() == OrderStatus.DELIVERED)
                deliveredCount++;
            else if (o.getStatus() == OrderStatus.CANCELLED)
                cancelledCount++;
            else if (o.getStatus() == OrderStatus.REFUSED)
                refusedCount++;
            else if (o.getStatus() == OrderStatus.DEFERRED)
                deferredCount++;

            if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REFUSED
                    || o.getStatus() == OrderStatus.DEFERRED
                    || o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                java.util.Map<String, Boolean> ctx = returnCtx.get(o.getId());
                if (ctx == null || !ctx.getOrDefault("hasConfirmed", false)) {
                    awaitingReceiptCount++;
                }
            }
        }

        model.addAttribute("totalOrderCount", orders.size());
        model.addAttribute("inTransitCount", inTransitCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("refusedCount", refusedCount);
        model.addAttribute("deferredCount", deferredCount);

        model.addAttribute("awaitingReceiptCount", awaitingReceiptCount);

        return "warehouse/external";
    }

    /**
     * تأكيد استلام أوردر مرتجع
     */
    @PostMapping("/confirm-receipt/{orderId}")
    public String confirmReceipt(@PathVariable Long orderId,
            @RequestParam Long businessDayId,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {

        try {
            orderService.confirmReturn(orderId, user);
            redirectAttributes.addFlashAttribute("success", "تم تأكيد استلام الأوردر بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/warehouse/" + businessDayId + "/external";
    }

    /**
     * عرض جميع طلبات العهدة للمنظمة بشكل موحد (Master View)
     */
    @GetMapping("/custody")
    public String viewCustody(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long incomingFromId,
            @RequestParam(required = false) Long outgoingToId,
            @RequestParam(required = false) OrderStatus status,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        // جلب الأوردرات بالفلاتر كاملة من الداتابيز
        java.util.List<Order> orders = orderService.getCustodyOrdersWithFilters(org.getId(), search, code, courierId,
                incomingFromId, outgoingToId, status, null);

        model.addAttribute("organization", org);
        model.addAttribute("orders", orders);
        model.addAttribute("search", search);
        model.addAttribute("code", code);
        model.addAttribute("courierId", courierId);
        model.addAttribute("incomingFromId", incomingFromId);
        model.addAttribute("outgoingToId", outgoingToId);
        model.addAttribute("status", status);

        // بيانات الفلاتر
        model.addAttribute("couriers", organizationService.getCouriers(org));
        model.addAttribute("linkedOrganizations", organizationService.getLinkedOrganizations(org));
        model.addAttribute("statuses", OrderStatus.values());

        // حساب سياق سلسلة الإسناد (للعرض فقط)
        java.util.Map<Long, java.util.Map<String, String>> chainCtx = orderService.getOrderChainContext(orders,
                org.getId());
        model.addAttribute("orderChainContext", chainCtx);

        java.util.Map<Long, java.util.Map<String, Boolean>> returnCtx = orderService.getOrderReturnContext(orders,
                org.getId());
        model.addAttribute("returnContext", returnCtx);

        // حساب الإحصائيات في دورة واحدة
        long waitingCount = 0, cancelledCount = 0, refusedCount = 0, deferredCount = 0, awaitingReceiptCount = 0;
        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.WAITING)
                waitingCount++;
            else if (o.getStatus() == OrderStatus.CANCELLED)
                cancelledCount++;
            else if (o.getStatus() == OrderStatus.REFUSED)
                refusedCount++;
            else if (o.getStatus() == OrderStatus.DEFERRED)
                deferredCount++;

            if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REFUSED
                    || o.getStatus() == OrderStatus.DEFERRED
                    || o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                java.util.Map<String, Boolean> ctx = returnCtx.get(o.getId());
                if (ctx == null || !ctx.getOrDefault("hasConfirmed", false)) {
                    awaitingReceiptCount++;
                }
            }
        }

        model.addAttribute("totalOrderCount", orders.size());
        model.addAttribute("waitingCount", waitingCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("refusedCount", refusedCount);
        model.addAttribute("deferredCount", deferredCount);
        model.addAttribute("awaitingReceiptCount", awaitingReceiptCount);

        return "warehouse/custody-day";
    }
}
