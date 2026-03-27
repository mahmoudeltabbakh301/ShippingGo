package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/shipment-requests")
public class ShipmentRequestController {

    private final OrderService orderService;
    private final OrganizationService organizationService;

    public ShipmentRequestController(OrderService orderService, OrganizationService organizationService) {
        this.orderService = orderService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String listRequests(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null || org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        java.time.LocalDate selectedDate = (date != null) ? date : java.time.LocalDate.now();

        List<OrderAssignment> assignments = orderService.getOrderAssignmentsByAssigneeAndDate(org.getId(), selectedDate);
        List<Order> pending = assignments.stream().filter(a -> !a.isAccepted()).map(OrderAssignment::getOrder).toList();
        List<Order> accepted = assignments.stream().filter(OrderAssignment::isAccepted).map(OrderAssignment::getOrder).toList();

        model.addAttribute("pendingRequests", pending);
        model.addAttribute("acceptedRequests", accepted);
        model.addAttribute("organization", org);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("previousDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("isToday", selectedDate.equals(java.time.LocalDate.now()));

        return "shipment-requests/list";
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.acceptAssignment(id, user);
            redirectAttributes.addFlashAttribute("success", "تم قبول الطلب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.rejectAssignment(id, user);
            redirectAttributes.addFlashAttribute("success", "تم رفض الطلب");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }

    @PostMapping("/bulk-action")
    public String bulkAction(@RequestParam(value = "selectedIds", required = false) List<Long> selectedIds,
            @RequestParam String action,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "لم يتم تحديد أي طلبات");
            return "redirect:/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
        }

        int successCount = 0;
        int errorCount = 0;

        for (Long id : selectedIds) {
            try {
                if ("accept".equals(action)) {
                    orderService.acceptAssignment(id, user);
                    successCount++;
                } else if ("reject".equals(action)) {
                    orderService.rejectAssignment(id, user);
                    successCount++;
                }
            } catch (Exception e) {
                errorCount++;
            }
        }

        if (successCount > 0) {
            redirectAttributes.addFlashAttribute("success", "تم تنفيذ الإجراء على " + successCount + " طلب بنجاح.");
        }
        if (errorCount > 0) {
            redirectAttributes.addFlashAttribute("error", "فشل معالجة " + errorCount + " طلب.");
        }

        return "redirect:/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }
}
