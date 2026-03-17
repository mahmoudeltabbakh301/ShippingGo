package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.ShipmentRequest;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.shipment.shippinggo.annotation.CurrentOrganization;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;

@Controller
@RequestMapping("/org/shipment-requests")
public class OrgShipmentRequestController {

    private final ShipmentRequestRepository shipmentRequestRepository;
    private final OrganizationService organizationService;
    private final com.shipment.shippinggo.service.BusinessDayService businessDayService;
    private final com.shipment.shippinggo.service.OrderService orderService;

    public OrgShipmentRequestController(ShipmentRequestRepository shipmentRequestRepository,
            OrganizationService organizationService,
            com.shipment.shippinggo.service.BusinessDayService businessDayService,
            com.shipment.shippinggo.service.OrderService orderService) {
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.organizationService = organizationService;
        this.businessDayService = businessDayService;
        this.orderService = orderService;
    }

    @GetMapping
    public String listRequests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null || org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        java.time.LocalDate selectedDate = (date != null) ? date : java.time.LocalDate.now();

        List<com.shipment.shippinggo.dto.IncomingAssignmentDTO> allRequests = new java.util.ArrayList<>();

        // 1. Fetch ShipmentRequests
        List<ShipmentRequest> requests = shipmentRequestRepository.findByOrganizationIdAndDate(org.getId(),
                selectedDate);
        for (ShipmentRequest req : requests) {
            allRequests.add(com.shipment.shippinggo.dto.IncomingAssignmentDTO.builder()
                    .id(req.getId()) // Keep internal ID
                    .referenceId(req.getId())
                    .type("REQUEST")
                    .senderName(req.getSenderName())
                    .senderPhone(req.getSenderPhone())
                    .recipientName(req.getRecipientName())
                    .recipientPhone(req.getRecipientPhone())
                    .recipientAddress(req.getRecipientAddress())
                    .contentDescription(req.getContentDescription())
                    .estimatedAmount(req.getEstimatedAmount())
                    .status(req.getStatus())
                    .createdAt(req.getCreatedAt())
                    .build());
        }

        // 2. Fetch Pending Orders (Assigned but not accepted)
        List<com.shipment.shippinggo.entity.Order> pendingOrders = orderService.getPendingAssignmentsByDate(org.getId(),
                selectedDate);
        for (com.shipment.shippinggo.entity.Order order : pendingOrders) {
            // Map OrderStatus.WAITING (or whatever status it has) to equivalent
            // For display, we can use the order status, but DTO expects
            // ShipmentRequestStatus
            // We can map loosely or just use the status field for display logic
            ShipmentRequestStatus displayStatus = ShipmentRequestStatus.PENDING; // Default for pending assignment

            allRequests.add(com.shipment.shippinggo.dto.IncomingAssignmentDTO.builder()
                    .id(order.getId())
                    .referenceId(order.getId())
                    .type("ORDER")
                    .recipientName(order.getRecipientName())
                    .recipientPhone(order.getRecipientPhone())
                    .recipientAddress(order.getRecipientAddress())
                    .contentDescription(order.getNotes()) // Use notes as description
                    .estimatedAmount(order.getAmount())
                    .status(displayStatus)
                    .createdAt(order.getCreatedAt())
                    .code(order.getCode())
                    .companyName(order.getCompanyName())
                    .senderName(order.getOwnerOrganization().getName()) // Sender is the owner org
                    .senderPhone(order.getOwnerOrganization().getPhone())
                    .build());
        }

        // Sort by date desc
        allRequests.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        model.addAttribute("requests", allRequests);
        model.addAttribute("organization", org);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("previousDate", selectedDate.minusDays(1));
        model.addAttribute("nextDate", selectedDate.plusDays(1));
        model.addAttribute("isToday", selectedDate.equals(java.time.LocalDate.now()));

        // Get today's business day for creating new shipments
        com.shipment.shippinggo.entity.BusinessDay today = businessDayService.getOrCreateTodayBusinessDay(org.getId(),
                user);
        model.addAttribute("todayBusinessDayId", today.getId());

        return "org/shipment-requests";
    }

    @PostMapping("/{id}/accept")
    public String acceptRequest(@PathVariable Long id, @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            if ("ORDER".equals(type)) {
                orderService.acceptAssignment(id, user); // Assuming OrderService already has necessary checks
                redirectAttributes.addFlashAttribute("success", "تم قبول الشحنة وإضافتها لليوم العمل.");
            } else {
                ShipmentRequest request = shipmentRequestRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Request not found"));

                // Security check (IDOR fix)
                if (!request.getOrganization().getId().equals(org.getId())) {
                    redirectAttributes.addFlashAttribute("error", "غير مصرح لك بتعديل هذا الطلب.");
                    return "redirect:/org/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
                }

                request.setStatus(ShipmentRequestStatus.ACCEPTED);
                shipmentRequestRepository.save(request);
                redirectAttributes.addFlashAttribute("success", "تم قبول الطلب. يرجى إنشاء شحنة رسمية له.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/org/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }

    @PostMapping("/{id}/reject")
    public String rejectRequest(@PathVariable Long id, @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            if ("ORDER".equals(type)) {
                orderService.rejectAssignment(id, user); // Assuming OrderService already has necessary checks
                redirectAttributes.addFlashAttribute("success", "تم رفض استلام الشحنة.");
            } else {
                ShipmentRequest request = shipmentRequestRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Request not found"));

                // Security check (IDOR fix)
                if (!request.getOrganization().getId().equals(org.getId())) {
                    redirectAttributes.addFlashAttribute("error", "غير مصرح لك بتعديل هذا الطلب.");
                    return "redirect:/org/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
                }

                request.setStatus(ShipmentRequestStatus.REJECTED);
                shipmentRequestRepository.save(request);
                redirectAttributes.addFlashAttribute("success", "تم رفض الطلب");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/org/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }

    @PostMapping("/bulk-action")
    public String bulkAction(@RequestParam(value = "selectedIds", required = false) List<String> selectedIds,
            @RequestParam String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null || org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        if (selectedIds == null || selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "لم يتم تحديد أي طلبات");
            return "redirect:/org/shipment-requests";
        }

        int successCount = 0;
        int errorCount = 0;

        for (String compoundId : selectedIds) {
            // Format: TYPE_ID (e.g., ORDER_123, REQUEST_456)
            String[] parts = compoundId.split("_");
            if (parts.length != 2)
                continue;

            String type = parts[0];
            Long id = Long.parseLong(parts[1]);

            try {
                if ("accept".equals(action)) {
                    if ("ORDER".equals(type)) {
                        orderService.acceptAssignment(id, user);
                    } else {
                        ShipmentRequest request = shipmentRequestRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Request not found"));
                        if (!request.getOrganization().getId().equals(org.getId()))
                            continue;
                        request.setStatus(ShipmentRequestStatus.ACCEPTED);
                        shipmentRequestRepository.save(request);
                    }
                    successCount++;
                } else if ("reject".equals(action)) {
                    if ("ORDER".equals(type)) {
                        orderService.rejectAssignment(id, user);
                    } else {
                        ShipmentRequest request = shipmentRequestRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Request not found"));
                        if (!request.getOrganization().getId().equals(org.getId()))
                            continue;
                        request.setStatus(ShipmentRequestStatus.REJECTED);
                        shipmentRequestRepository.save(request);
                    }
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

        return "redirect:/org/shipment-requests" + (date != null ? "?date=" + date.toString() : "");
    }
}
