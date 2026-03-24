package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.ExcelExportService;
import com.shipment.shippinggo.service.OrderLabelService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/business-days")
public class BusinessDayController {

    private final BusinessDayService businessDayService;
    private final OrganizationService organizationService;
    private final OrderService orderService;
    private final ExcelExportService excelExportService;
    private final VirtualOfficeRepository virtualOfficeRepository;
    private final OrderLabelService orderLabelService;

    public BusinessDayController(BusinessDayService businessDayService,
            OrganizationService organizationService,
            OrderService orderService,
            ExcelExportService excelExportService,
            VirtualOfficeRepository virtualOfficeRepository,
            OrderLabelService orderLabelService) {
        this.businessDayService = businessDayService;
        this.organizationService = organizationService;
        this.orderService = orderService;
        this.excelExportService = excelExportService;
        this.virtualOfficeRepository = virtualOfficeRepository;
        this.orderLabelService = orderLabelService;
    }

    @GetMapping
    public String listBusinessDays(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {
        model.addAttribute("businessDays", businessDayService.getBusinessDaysForUser(org.getId(), user));
        model.addAttribute("organization", org);
        return "business-days/list";
    }

    @GetMapping("/new")
    public String showNewForm(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {
        model.addAttribute("organization", org);
        model.addAttribute("today", LocalDate.now());
        return "business-days/new";
    }

    @PostMapping("/new")
    public String createBusinessDay(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String name,
            RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مسموح");
            return "redirect:/members/invitations";
        }

        try {
            var bd = businessDayService.createBusinessDay(org.getId(), date, name, user);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء يوم العمل بنجاح");
            return "redirect:/business-days/" + bd.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Long id, @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            var bd = businessDayService.getById(id);
            if (bd != null && !bd.getOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح");
                return "redirect:/business-days";
            }
            businessDayService.toggleActive(id, user);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة يوم العمل");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days";
    }

    @PostMapping("/{id}/delete")
    public String deleteBusinessDay(@PathVariable Long id, @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: المشرف أو المدير فقط يمكنه حذف أيام العمل");
            return "redirect:/business-days/" + id;
        }

        try {
            var bd = businessDayService.getById(id);
            if (bd != null && !bd.getOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح بمسح يوم العمل هذا");
                return "redirect:/business-days/" + id;
            }
            businessDayService.deleteBusinessDay(id, user); // cascading delete is handled in service
            redirectAttributes.addFlashAttribute("success", "تم حذف يوم العمل وجميع طلباته بنجاح");
            return "redirect:/business-days";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل حذف يوم العمل: " + e.getMessage());
            return "redirect:/business-days/" + id;
        }
    }

    @PostMapping("/{id}/delete-orders")
    public String deleteOrders(@PathVariable Long id, @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: المشرف أو المدير فقط يمكنه حذف الطلبات");
            return "redirect:/business-days/" + id;
        }

        try {
            var bd = businessDayService.getById(id);
            if (bd != null && !bd.getOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح بمسح طلبات يوم العمل هذا");
                return "redirect:/business-days/" + id;
            }
            orderService.deleteOrdersByBusinessDay(id, user);
            redirectAttributes.addFlashAttribute("success", "تم حذف جميع طلبات اليوم بنجاح");
            return "redirect:/business-days/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل حذف الطلبات: " + e.getMessage());
            return "redirect:/business-days/" + id;
        }
    }

    @GetMapping("/{id}")
    public String viewBusinessDay(@PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long incomingFromId,
            @RequestParam(required = false) Long outgoingToId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Governorate governorate,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        var businessDay = businessDayService.getById(id);
        if (businessDay == null) {
            return "redirect:/business-days";
        }

        // === Security Check: التحقق من أن يوم العمل يتبع لمنظمة المستخدم ===
        if (!businessDay.getOrganization().getId().equals(org.getId())) {
            return "redirect:/business-days";
        }

        // جلب الأوردرات بالفلاتر كاملة من الداتابيز (تجنب N+1 والـ Stream Filtering)
        List<Order> orders = orderService.getOrdersByBusinessDayWithFullFilters(id, org.getId(), search, code, courierId, 
                incomingFromId, outgoingToId, status, governorate);

        // حساب سياق سلسلة الإسناد (للعرض فقط الآن وليس للفلترة)
        var chainContext = orderService.getOrderChainContext(orders, org.getId());

        model.addAttribute("businessDay", businessDay);
        model.addAttribute("organization", org);
        model.addAttribute("orders", orders);
        model.addAttribute("orderChainContext", chainContext);
        model.addAttribute("search", search);
        model.addAttribute("code", code);
        model.addAttribute("courierId", courierId);
        model.addAttribute("incomingFromId", incomingFromId);
        model.addAttribute("outgoingToId", outgoingToId);
        model.addAttribute("status", status);
        model.addAttribute("governorate", governorate);

        // Data for filter dropdowns — كل المنظمات المرتبطة
        model.addAttribute("couriers", organizationService.getCouriers(org));
        List<com.shipment.shippinggo.entity.Organization> linkedOrgs = organizationService.getLinkedOrganizations(org);
        model.addAttribute("linkedOrganizations", linkedOrgs);

        // كمان نضيف الشركات والمكاتب للإسناد الجماعي
        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            model.addAttribute("offices", organizationService.getOfficesByCompany(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            model.addAttribute("companies", organizationService.getCompaniesByStore(org.getId()));
            model.addAttribute("offices", organizationService.getOfficesByStore(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            model.addAttribute("offices", organizationService.getLinkedOffices(org.getId()));
        } else {
            model.addAttribute("offices", java.util.Collections.emptyList());
        }
        model.addAttribute("virtualOffices", virtualOfficeRepository.findByParentOrganizationId(org.getId()));

        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("governorates", Governorate.values());

        // Statistics for this day (based on list)
        model.addAttribute("totalOrderCount", orders.size());
        
        double totalAmt = 0;
        double deliveredAmt = 0;
        long waiting = 0, inTransit = 0, delivered = 0, cancelled = 0, refused = 0, deferred = 0;

        for (Order o : orders) {
            double amt = o.getAmount() != null ? o.getAmount().doubleValue() : 0;
            totalAmt += amt;
            
            if (o.getStatus() == OrderStatus.DELIVERED) {
                deliveredAmt += amt;
                delivered++;
            } else if (o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                deliveredAmt += (o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount().doubleValue() : 0);
                delivered++;
            } else if (o.getStatus() == OrderStatus.WAITING) waiting++;
            else if (o.getStatus() == OrderStatus.IN_TRANSIT) inTransit++;
            else if (o.getStatus() == OrderStatus.CANCELLED) cancelled++;
            else if (o.getStatus() == OrderStatus.REFUSED) refused++;
            else if (o.getStatus() == OrderStatus.DEFERRED) deferred++;
        }

        model.addAttribute("totalAmount", totalAmt);
        model.addAttribute("deliveredAmount", deliveredAmt);
        model.addAttribute("waitingCount", waiting);
        model.addAttribute("inTransitCount", inTransit);
        model.addAttribute("deliveredCount", delivered);
        model.addAttribute("cancelledCount", cancelled);
        model.addAttribute("refusedCount", refused);
        model.addAttribute("deferredCount", deferred);

        // Pending memberships for Admin
        if (user.getRole() == Role.ADMIN) {
            model.addAttribute("pendingMemberships", organizationService.getPendingMemberships(org.getId()));
        }

        return "business-days/view";
    }

    /**
     * عرض حسابات يوم عمل محدد
     */
    @GetMapping("/{id}/accounts")
    public String viewBusinessDayAccounts(@PathVariable Long id,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        var businessDay = businessDayService.getById(id);
        if (businessDay == null || !businessDay.getOrganization().getId().equals(org.getId())) {
            return "redirect:/business-days";
        }

        return "redirect:/accounts/day/" + id;
    }

    @PostMapping("/{id}/update-name")
    public String updateName(@PathVariable Long id, @RequestParam String name,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: المشرف أو المدير فقط يمكنه تعديل اسم يوم العمل");
            return "redirect:/business-days";
        }
        try {
            var bd = businessDayService.getById(id);
            if (bd != null && !bd.getOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح بتعديل يوم العمل هذا");
                return "redirect:/business-days";
            }
            businessDayService.updateName(id, name);
            redirectAttributes.addFlashAttribute("success", "تم تحديث اسم يوم العمل بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ أثناء تحديث الاسم: " + e.getMessage());
        }
        return "redirect:/business-days";
    }

    /**
     * تصدير طلبات يوم عمل محدد إلى Excel مع تطبيق نفس الفلاتر المستخدمة في العرض
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportBusinessDayOrders(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long incomingFromId,
            @RequestParam(required = false) Long outgoingToId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Governorate governorate,
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user) {

        var businessDay = businessDayService.getById(id);
        if (businessDay == null || !businessDay.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        // استخدام نفس الميثود المستخدمة في عرض الصفحة لضمان تطابق النتائج
        List<Order> orders = orderService.getOrdersByBusinessDayWithFullFilters(
                id, org.getId(), search, code, courierId,
                incomingFromId, outgoingToId, status, governorate);

        try {
            java.io.ByteArrayInputStream in = excelExportService.exportOrdersToExcel(orders);
            org.springframework.core.io.InputStreamResource resource =
                    new org.springframework.core.io.InputStreamResource(in);

            String filename = "orders_" + businessDay.getDate().toString() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/bulk-print-labels")
    public ResponseEntity<byte[]> bulkPrintLabels(@PathVariable Long id,
            @RequestParam List<Long> orderIds,
            @CurrentOrganization Organization org) {

        var businessDay = businessDayService.getById(id);
        if (businessDay == null || !businessDay.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<Order> orders = orderIds.stream()
                    .map(orderService::getById)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toList());
            byte[] pdfBytes = orderLabelService.generateBulkLabels(orders);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "bulk_labels_bd_" + id + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
