package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MembershipController {

    private final OrganizationService organizationService;

    public MembershipController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    // === Admin Side: Members Management ===

    @GetMapping
    public String listMembers(@AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/members/invitations";
        }

        model.addAttribute("organization", org);
        model.addAttribute("pendingMemberships", organizationService.getPendingMemberships(org.getId()));
        model.addAttribute("acceptedMemberships", organizationService.getAcceptedMemberships(org.getId()));
        model.addAttribute("couriers", organizationService.getCouriers(org));
        model.addAttribute("dataEntryUsers", organizationService.getDataEntryUsers(org));
        return "members/list";
    }

    @PostMapping("/invite")
    public String inviteMember(@AuthenticationPrincipal User user,
            @RequestParam String identifier,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                redirectAttributes.addFlashAttribute("error", "لا يمكنك إرسال دعوات بدون منظمة");
                return "redirect:/members";
            }
            organizationService.inviteMember(org, identifier, role, user);
            redirectAttributes.addFlashAttribute("success", "تم إرسال الدعوة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/cancel-invite")
    public String cancelInvitation(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.cancelInvitation(id);
            redirectAttributes.addFlashAttribute("success", "تم إلغاء الدعوة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/update-role")
    public String updateRole(@PathVariable Long id,
            @RequestParam Role role,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.changeMemberRole(id, role);
            redirectAttributes.addFlashAttribute("success", "تم تحديث وظيفة العضو بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/remove")
    public String removeMember(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.removeMember(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف العضو بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members";
    }

    // === Member Side: Invitations ===

    @GetMapping("/invitations")
    public String invitations(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("pendingInvitations", organizationService.getPendingInvitationsForUser(user));
        return "members/invitations";
    }

    @PostMapping("/invitations/{id}/accept")
    public String acceptInvitation(@PathVariable Long id,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.acceptInvitation(id, user);
            redirectAttributes.addFlashAttribute("success", "تم قبول الدعوة بنجاح! يمكنك الآن ممارسة مهام عملك.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members/invitations";
    }

    @PostMapping("/invitations/{id}/decline")
    public String declineInvitation(@PathVariable Long id,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.declineInvitation(id, user);
            redirectAttributes.addFlashAttribute("success", "تم رفض الدعوة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members/invitations";
    }
}
