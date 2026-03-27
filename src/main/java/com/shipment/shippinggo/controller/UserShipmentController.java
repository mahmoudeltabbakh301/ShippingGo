package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/user/ship")
public class UserShipmentController {

    private final OrganizationService organizationService;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final UserService userService;

    public UserShipmentController(OrganizationService organizationService,
            ShipmentRequestRepository shipmentRequestRepository,
            UserService userService) {
        this.organizationService = organizationService;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.userService = userService;
    }

    @GetMapping
    public String showShipmentPage(@AuthenticationPrincipal User user, Model model) {
        List<Organization> availableOrgs = organizationService.findAvailableOrganizationsForMember(user);

        List<Company> companies = availableOrgs.stream()
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY)
                .map(org -> (Company) org)
                .toList();

        List<Office> offices = availableOrgs.stream()
                .filter(org -> org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE)
                .map(org -> (Office) org)
                .toList();

        model.addAttribute("companies", companies);
        model.addAttribute("offices", offices);
        model.addAttribute("user", user);

        return "user/ship";
    }

    @GetMapping("/request/{orgId}")
    public String showRequestForm(@PathVariable Long orgId, @AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getById(orgId);
        if (org == null) {
            return "redirect:/user/ship";
        }

        model.addAttribute("organization", org);
        model.addAttribute("user", user);
        return "user/ship-form";
    }

    @PostMapping("/request")
    public String submitRequest(@AuthenticationPrincipal User user,
            @RequestParam Long organizationId,
            @RequestParam String senderName,
            @RequestParam String senderPhone,
            @RequestParam String recipientName,
            @RequestParam String recipientPhone,
            @RequestParam String recipientAddress,
            @RequestParam(required = false) String contentDescription,
            @RequestParam(required = false) BigDecimal estimatedAmount,
            RedirectAttributes redirectAttributes) {
        try {
            Organization org = organizationService.getById(organizationId);
            if (org == null) {
                throw new RuntimeException("Organization not found");
            }

            ShipmentRequest request = ShipmentRequest.builder()
                    .requester(user)
                    .organization(org)
                    .senderName(senderName)
                    .senderPhone(senderPhone)
                    .recipientName(recipientName)
                    .recipientPhone(recipientPhone)
                    .recipientAddress(recipientAddress)
                    .contentDescription(contentDescription)
                    .estimatedAmount(estimatedAmount)
                    .status(ShipmentRequestStatus.PENDING)
                    .build();

            shipmentRequestRepository.save(request);
            redirectAttributes.addFlashAttribute("success", "تم إرسال طلب الشحن بنجاح");
            return "redirect:/user/my-orders"; // We'll create this next
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/user/ship/request/" + organizationId;
        }
    }
}
