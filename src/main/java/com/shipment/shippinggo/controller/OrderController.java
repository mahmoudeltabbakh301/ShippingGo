package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import com.shipment.shippinggo.repository.VirtualOfficeRepository;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final BusinessDayService businessDayService;
    private final OrganizationService organizationService;
    private final ExcelImportService excelImportService;
    private final ExcelExportService excelExportService;
    private final OrderLabelService orderLabelService;
    private final QrCodeService qrCodeService;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public OrderController(OrderService orderService,
            BusinessDayService businessDayService,
            OrganizationService organizationService,
            ExcelImportService excelImportService,
            ExcelExportService excelExportService,
            OrderLabelService orderLabelService,
            QrCodeService qrCodeService,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.orderService = orderService;
        this.businessDayService = businessDayService;
        this.organizationService = organizationService;
        this.excelImportService = excelImportService;
        this.excelExportService = excelExportService;
        this.orderLabelService = orderLabelService;
        this.qrCodeService = qrCodeService;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    @GetMapping("/orders")
    public String listOrders(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) OrderStatus status,
            Model model) {

        java.util.List<com.shipment.shippinggo.entity.Order> orders;

        // Use search param as code if provided (legacy support) or use new filters
        String searchCode = (search != null && !search.isEmpty()) ? search : code;

        boolean hasFilter = (searchCode != null && !searchCode.trim().isEmpty())
                || courierId != null
                || officeId != null
                || status != null;

        if (hasFilter) {
            orders = orderService.filterOrders(org.getId(), searchCode, courierId, officeId, status, null);
        } else {
            BusinessDay currentDay = businessDayService.getOrCreateTodayBusinessDay(org.getId(), user);
            orders = orderService.getOrdersByBusinessDay(currentDay.getId());
        }

        // Calculate statistics
        long totalOrders = orders.size();
        java.math.BigDecimal totalAmount = orders.stream()
                .map(o -> o.getAmount() != null ? o.getAmount() : java.math.BigDecimal.ZERO)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Fetch data for filters
        model.addAttribute("couriers", organizationService.getCouriers(org));
        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            model.addAttribute("offices", organizationService.getOfficesByCompany(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            model.addAttribute("companies", organizationService.getCompaniesByStore(org.getId()));
            model.addAttribute("offices", organizationService.getOfficesByStore(org.getId()));
        } else {
            // Offices might see linked offices, simpler to show linked offices if needed or
            // just empty for now
            model.addAttribute("offices", organizationService.getLinkedOffices(org.getId()));
        }
        model.addAttribute("virtualOffices", virtualOfficeRepository.findByParentOrganizationId(org.getId()));

        model.addAttribute("orders", orders);
        model.addAttribute("orderChainContext", orderService.getOrderChainContext(orders, org.getId()));
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalAmount", totalAmount);

        // Pass filter values back to view
        model.addAttribute("search", search);
        model.addAttribute("code", searchCode);
        model.addAttribute("courierId", courierId);
        model.addAttribute("officeId", officeId);
        model.addAttribute("status", status);

        model.addAttribute("organization", org);
        model.addAttribute("statuses", OrderStatus.values());
        return "orders/list";
    }

    @GetMapping("/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportOrders(
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) OrderStatus status) {

        String searchCode = (search != null && !search.isEmpty()) ? search : code;

        boolean hasFilter = (searchCode != null && !searchCode.trim().isEmpty())
                || courierId != null
                || officeId != null
                || status != null;

        java.util.List<com.shipment.shippinggo.entity.Order> orders;
        if (hasFilter) {
            orders = orderService.filterOrders(org.getId(), searchCode, courierId, officeId, status, null);
        } else {
            BusinessDay currentDay = businessDayService.getOrCreateTodayBusinessDay(org.getId(), user);
            orders = orderService.getOrdersByBusinessDay(currentDay.getId());
        }

        try {
            java.io.ByteArrayInputStream in = excelExportService.exportOrdersToExcel(orders);
            org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(
                    in);

            String filename = "orders_" + java.time.LocalDate.now().toString() + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (java.io.IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/new")
    public String showNewOrderForm(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) String recipientName,
            @RequestParam(required = false) String recipientPhone,
            @RequestParam(required = false) String recipientAddress,
            @RequestParam(required = false) java.math.BigDecimal shippingPrice,
            @RequestParam(required = false) java.math.BigDecimal orderPrice,
            @RequestParam(required = false) java.math.BigDecimal amount,
            @RequestParam(required = false) String notes,
            Model model) {

        BusinessDay businessDay;
        if (businessDayId != null) {
            businessDay = businessDayService.getById(businessDayId);
            // Verify business day belongs to organization (optional but good security)
            if (businessDay == null || !businessDay.getOrganization().getId().equals(org.getId())) {
                return "redirect:/members/invitations";
            }
        } else {
            // Fallback to today behavior if no ID provided
            businessDay = businessDayService.getOrCreateTodayBusinessDay(org.getId(), user);
        }

        OrderDto dto = new OrderDto();
        dto.setBusinessDayId(businessDay.getId());

        // Pre-fill data if provided
        if (recipientName != null)
            dto.setRecipientName(recipientName);
        if (recipientPhone != null)
            dto.setRecipientPhone(recipientPhone);
        if (recipientAddress != null)
            dto.setRecipientAddress(recipientAddress);
        if (shippingPrice != null)
            dto.setShippingPrice(shippingPrice);
        if (orderPrice != null)
            dto.setOrderPrice(orderPrice);
        if (amount != null)
            dto.setAmount(amount);
        if (notes != null)
            dto.setNotes(notes);

        model.addAttribute("orderDto", dto);
        model.addAttribute("businessDay", businessDay);
        model.addAttribute("organization", org);
        model.addAttribute("governorates", Governorate.values());
        return "orders/new";
    }

    @PostMapping("/new")
    public String createOrder(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @ModelAttribute OrderDto orderDto,
            RedirectAttributes redirectAttributes) {

        try {
            orderService.createOrder(orderDto, user, org);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء الطلب بنجاح");

            if (orderDto.getBusinessDayId() != null) {
                return "redirect:/business-days/" + orderDto.getBusinessDayId();
            }
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // Redirect back to the form with the specific business day if available
            if (orderDto.getBusinessDayId() != null) {
                return "redirect:/orders/new?businessDayId=" + orderDto.getBusinessDayId();
            }
            return "redirect:/orders/new";
        }
    }

    @GetMapping("/import")
    public String showImportForm(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {

        model.addAttribute("organization", org);
        model.addAttribute("businessDays", businessDayService.getBusinessDays(org.getId()));

        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            model.addAttribute("offices", organizationService.getOfficesByCompany(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            model.addAttribute("companies", organizationService.getCompaniesByStore(org.getId()));
            model.addAttribute("offices", organizationService.getOfficesByStore(org.getId()));
        } else {
            model.addAttribute("offices", organizationService.getLinkedOffices(org.getId()));
        }
        model.addAttribute("virtualOffices", virtualOfficeRepository.findByParentOrganizationId(org.getId()));

        return "orders/import";
    }

    @PostMapping("/import")
    public String importOrders(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam("businessDayId") Long businessDayId,
            @RequestParam(value = "assignedToOrganizationId", required = false) Long assignedToOrganizationId,
            RedirectAttributes redirectAttributes) {

        BusinessDay businessDay = businessDayService.getById(businessDayId);
        if (businessDay == null || !businessDay.getOrganization().getId().equals(org.getId())) {
            redirectAttributes.addFlashAttribute("error", "يوم العمل غير موجود أو غير مصرح");
            return "redirect:/business-days";
        }

        var result = excelImportService.importOrders(file, businessDay, user, org, assignedToOrganizationId);

        if (result.getSuccessCount() > 0) {
            redirectAttributes.addFlashAttribute("success",
                    "تم استيراد " + result.getSuccessCount() + " طلب بنجاح");
        }
        if (result.getErrorCount() > 0) {
            redirectAttributes.addFlashAttribute("warning",
                    "فشل استيراد " + result.getErrorCount() + " طلب");
            redirectAttributes.addFlashAttribute("errors", result.getErrors());
        }

        return "redirect:/business-days/" + businessDayId;
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {
        var order = orderService.getById(id);
        if (order == null) {
            return "redirect:/orders";
        }

        // === Security Check: التحقق من صلاحية الوصول للطلب ===
        if (!orderService.canUserAccessOrder(user, order)) {
            return "redirect:/dashboard";
        }

        if (org != null) {
            model.addAttribute("couriers", organizationService.getCouriers(org));
            if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
                // Companies see their child offices and linked offices
                model.addAttribute("offices", organizationService.getOfficesByCompany(org.getId()));
            } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                // Offices see their linked peer offices
                model.addAttribute("offices", organizationService.getLinkedOffices(org.getId()));
            } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
                // Stores see linked companies to assign to
                model.addAttribute("companies", organizationService.getCompaniesByStore(org.getId()));
                model.addAttribute("offices", organizationService.getOfficesByStore(org.getId()));
            } else {
                model.addAttribute("offices", java.util.Collections.emptyList());
            }
            model.addAttribute("virtualOffices", virtualOfficeRepository.findByParentOrganizationId(org.getId()));
            model.addAttribute("currentUserOrganization", org);
        }

        model.addAttribute("order", order);
        model.addAttribute("businessDayId", businessDayId);
        model.addAttribute("history", orderService.getOrderHistory(id));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("governorates", Governorate.values());

        // إضافة حالة الإرجاع للطلبات المرفوضة والملغية
        if (order.getStatus() == OrderStatus.REFUSED || order.getStatus() == OrderStatus.CANCELLED) {
            model.addAttribute("returnStatus", orderService.getReturnStatus(id));
        }

        return "orders/view";
    }

    @PostMapping("/{id}/assign-courier")
    public String assignToCourier(@PathVariable Long id,
            @RequestParam Long courierId,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        try {
            orderService.assignToCourier(id, courierId, user);
            redirectAttributes.addFlashAttribute("success", "تم إسناد الطلب للمندوب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
    }

    @PostMapping("/{id}/unassign-courier")
    public String unassignCourier(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        // حفظ معرف يوم العمل قبل إلغاء الإسناد
        Long bdId = businessDayId != null ? businessDayId
                : (order.getBusinessDay() != null ? order.getBusinessDay().getId() : null);
        try {
            orderService.unassignCourier(id, user);
            redirectAttributes.addFlashAttribute("success", "تم إلغاء إسناد الطلب من المندوب");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (bdId != null) {
            return "redirect:/orders/" + id + "?businessDayId=" + bdId;
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/assign-organization")
    public String assignToOrganization(@PathVariable Long id,
            @RequestParam Long organizationId,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        try {
            orderService.assignToOrganization(id, organizationId, user);
            redirectAttributes.addFlashAttribute("success", "تم إسناد الطلب للمكتب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
    }

    @PostMapping("/{id}/unassign-organization")
    public String unassignOrganization(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        // حفظ معرف يوم العمل قبل إلغاء الإسناد
        Long bdId = businessDayId != null ? businessDayId
                : (order.getBusinessDay() != null ? order.getBusinessDay().getId() : null);
        try {
            orderService.unassignOrganization(id, user);
            redirectAttributes.addFlashAttribute("success", "تم إلغاء إسناد الطلب من المكتب");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (bdId != null) {
            return "redirect:/orders/" + id + "?businessDayId=" + bdId;
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/assign-custody")
    public String assignToCustody(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        Long bdId = businessDayId != null ? businessDayId
                : (order.getBusinessDay() != null ? order.getBusinessDay().getId() : null);
        try {
            orderService.assignToCustody(id, user);
            redirectAttributes.addFlashAttribute("success", "تم نقل الطلب لعهدة المخزن بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (bdId != null) {
            return "redirect:/orders/" + id + "?businessDayId=" + bdId;
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/remove-custody")
    public String removeFromCustody(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        Long bdId = businessDayId != null ? businessDayId
                : (order.getBusinessDay() != null ? order.getBusinessDay().getId() : null);
        try {
            orderService.removeFromCustody(id, user);
            redirectAttributes.addFlashAttribute("success", "تم إزالة الطلب من العهدة وإعادته للمخزن بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (bdId != null) {
            return "redirect:/orders/" + id + "?businessDayId=" + bdId;
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/confirm-return")
    public String confirmReturn(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }
        try {
            orderService.confirmReturn(id, user);
            redirectAttributes.addFlashAttribute("success", "تم تأكيد استلام المرتجع بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
    }

    /**
     * إسناد متعدد للمكتب
     */
    @PostMapping("/bulk-assign-organization")
    public String bulkAssignToOrganization(
            @RequestParam String orderIds,
            @RequestParam Long organizationId,
            @RequestParam Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            String[] ids = orderIds.split(",");
            List<Long> idList = new java.util.ArrayList<>();
            for (String idStr : ids) {
                try {
                    idList.add(Long.parseLong(idStr.trim()));
                } catch (Exception ignored) {
                }
            }
            if (!idList.isEmpty()) {
                orderService.bulkAssignToOrganization(idList, organizationId, user);
                redirectAttributes.addFlashAttribute("success",
                        "تم إسناد الطلبات للمكتب بنجاح");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days/" + businessDayId;
    }

    /**
     * إزالة متعددة من العهدة
     */
    @PostMapping("/bulk-remove-custody")
    public String bulkRemoveFromCustody(
            @RequestParam String orderIds,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            String[] ids = orderIds.split(",");
            int successCount = 0;
            for (String idStr : ids) {
                try {
                    Long orderId = Long.parseLong(idStr.trim());
                    var order = orderService.getById(orderId);
                    if (order != null && orderService.canUserAccessOrder(user, order)) {
                        orderService.removeFromCustody(orderId, user);
                        successCount++;
                    }
                } catch (Exception ignored) {
                }
            }
            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        "تم إزالة " + successCount + " طلب من العهدة بنجاح");
            } else {
                redirectAttributes.addFlashAttribute("error", "لم يتم نقل أي طلب");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/warehouse/custody";
    }

    /**
     * إسناد متعدد للمندوب
     */
    @PostMapping("/bulk-assign-courier")
    public String bulkAssignToCourier(
            @RequestParam String orderIds,
            @RequestParam Long courierId,
            @RequestParam Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            String[] ids = orderIds.split(",");
            List<Long> idList = new java.util.ArrayList<>();
            for (String idStr : ids) {
                try {
                    idList.add(Long.parseLong(idStr.trim()));
                } catch (Exception ignored) {
                }
            }
            if (!idList.isEmpty()) {
                orderService.bulkAssignToCourier(idList, courierId, user);
                redirectAttributes.addFlashAttribute("success",
                        "تم إسناد الطلبات للمندوب بنجاح");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days/" + businessDayId;
    }

    /**
     * إسناد متعدد للعهدة
     */
    @PostMapping("/bulk-assign-custody")
    public String bulkAssignToCustody(
            @RequestParam String orderIds,
            @RequestParam Long businessDayId,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            String[] ids = orderIds.split(",");
            int successCount = 0;
            for (String idStr : ids) {
                try {
                    Long id = Long.parseLong(idStr.trim());
                    orderService.assignToCustody(id, user);
                    successCount++;
                } catch (Exception ignored) {
                }
            }
            if (successCount > 0) {
                redirectAttributes.addFlashAttribute("success",
                        "تم إسناد " + successCount + " طلب للعهدة بنجاح");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days/" + businessDayId;
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam OrderStatus status,
            @RequestParam(required = false) java.math.BigDecimal amount,
            @RequestParam(required = false) java.math.BigDecimal rejectionPayment,
            @RequestParam(required = false) Integer deliveredPieces,
            @RequestParam(required = false) java.math.BigDecimal partialDeliveryAmount,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {

        // === Security Check: التحقق من صلاحية الوصول للطلب ===
        var orderCheck = orderService.getById(id);
        if (orderCheck == null || !orderService.canUserAccessOrder(user, orderCheck)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }

        // Check permissions
        if (user.getRole() == com.shipment.shippinggo.enums.Role.ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح للمحاسب بتعديل الطلبات");
            return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
        }

        try {
            orderService.updateStatusAdvanced(id, status, amount, rejectionPayment, deliveredPieces,
                    partialDeliveryAmount, user, notes);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة الطلب بنجاح");

            if (user.getRole() == com.shipment.shippinggo.enums.Role.COURIER) {
                return "redirect:/dashboard/courier";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (user.getRole() == com.shipment.shippinggo.enums.Role.COURIER) {
                return "redirect:/dashboard/courier";
            }
        }
        return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
    }

    /**
     * تحديث معلومات الطلب
     */
    @PostMapping("/{id}/update-info")
    public String updateOrderInfo(@PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam String recipientName,
            @RequestParam(required = false) String recipientPhone,
            @RequestParam(required = false) String recipientAddress,
            @RequestParam(required = false) java.math.BigDecimal shippingPrice,
            @RequestParam(required = false) java.math.BigDecimal orderPrice,
            @RequestParam(required = false) java.math.BigDecimal amount,
            @RequestParam(required = false) java.math.BigDecimal rejectionPayment,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) Governorate governorate,
            @RequestParam(required = false) Integer quantity,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {

        // === Security Check: التحقق من صلاحية الوصول للطلب ===
        var orderCheck = orderService.getById(id);
        if (orderCheck == null || !orderService.canUserAccessOrder(user, orderCheck)) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بالوصول لهذا الطلب");
            return "redirect:/dashboard";
        }

        if (user.getRole() == com.shipment.shippinggo.enums.Role.ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح للمحاسب بتعديل بيانات الطلب");
            return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
        }
        try {
            OrderDto dto = new OrderDto();
            dto.setRecipientName(recipientName);
            dto.setRecipientPhone(recipientPhone);
            dto.setRecipientAddress(recipientAddress);
            dto.setShippingPrice(shippingPrice);
            dto.setOrderPrice(orderPrice);
            dto.setAmount(amount);
            dto.setRejectionPayment(rejectionPayment);
            dto.setNotes(notes);
            dto.setCode(code);
            dto.setCompanyName(companyName);
            dto.setGovernorate(governorate);
            dto.setQuantity(quantity);

            orderService.updateOrderDetails(id, dto, user);

            redirectAttributes.addFlashAttribute("success", "تم تحديث معلومات الطلب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل تحديث الطلب: " + e.getMessage());
        }
        return "redirect:/orders/" + id + (businessDayId != null ? "?businessDayId=" + businessDayId : "");
    }

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id,
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (user.getRole() != com.shipment.shippinggo.enums.Role.ADMIN
                && user.getRole() != com.shipment.shippinggo.enums.Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: المشرف أو المدير فقط يمكنه حذف الطلبات");
            return "redirect:/orders/" + id;
        }

        // Get business day ID before deleting the order
        var order = orderService.getById(id);

        // === حماية: التحقق من أن الأوردر يتبع لمنظمة المستخدم ===
        if (order != null && !order.getOwnerOrganization().getId().equals(org.getId())) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: لا يمكنك حذف طلب لمنظمة أخرى");
            return "redirect:/orders";
        }

        Long businessDayId = order != null && order.getBusinessDay() != null ? order.getBusinessDay().getId() : null;

        try {
            orderService.deleteOrder(id, user);
            redirectAttributes.addFlashAttribute("success", "تم حذف الطلب بنجاح");
            if (businessDayId != null) {
                // Return to the day only if we successfully deleted it (implies
                // ownership/access)
                return "redirect:/business-days/" + businessDayId;
            }
            return "redirect:/orders";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل حذف الطلب: " + e.getMessage());
            // On error, redirect to generic orders page to avoid exposing other org's days
            // or if the user doesn't have access
            return "redirect:/orders";
        }
    }

    @GetMapping("/{id}/label")
    public ResponseEntity<byte[]> downloadOrderLabel(@PathVariable Long id,
            @AuthenticationPrincipal User user) {
        var order = orderService.getById(id);
        if (order == null || !orderService.canUserAccessOrder(user, order)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] pdfBytes = orderLabelService.generateOrderLabel(order);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"order-" + order.getCode() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/business-days/{bdId}/print-labels")
    public ResponseEntity<byte[]> downloadBusinessDayLabels(@PathVariable Long bdId,
            @AuthenticationPrincipal User user) {
        BusinessDay bd = businessDayService.getById(bdId);
        if (bd == null) {
            return ResponseEntity.notFound().build();
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null || (!bd.getOrganization().getId().equals(org.getId())
                && user.getRole() != com.shipment.shippinggo.enums.Role.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }

        java.util.List<com.shipment.shippinggo.entity.Order> orders = orderService.getOrdersByBusinessDay(bd.getId());
        if (orders.isEmpty()) {
            return ResponseEntity.ok().body(new byte[0]);
        }

        try {
            byte[] pdfBytes = orderLabelService.generateBulkLabels(orders);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"labels-" + bd.getDate() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
