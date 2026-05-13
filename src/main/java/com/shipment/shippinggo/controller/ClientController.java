package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.ClientOrg;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final OrganizationService organizationService;

    public ClientController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public String listClients(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {
        if (org == null) {
            return "redirect:/members/invitations";
        }

        // فقط الشركات والمكاتب يمكنها رؤية صفحة العملاء
        if (org.getType() != com.shipment.shippinggo.enums.OrganizationType.COMPANY
                && org.getType() != com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            return "redirect:/dashboard";
        }

        List<ClientOrg> clients = organizationService.getClientsByOrganization(org.getId());
        List<Membership> pendingInvitations = organizationService.getPendingClientInvitations(org.getId());

        model.addAttribute("organization", org);
        model.addAttribute("clients", clients);
        model.addAttribute("pendingInvitations", pendingInvitations);

        return "clients/list";
    }

    @PostMapping("/invite")
    public String inviteClient(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam String identifier,
            RedirectAttributes redirectAttributes) {
        try {
            if (org == null) {
                redirectAttributes.addFlashAttribute("error", "لا يمكنك إرسال دعوات بدون منظمة");
                return "redirect:/clients";
            }
            organizationService.inviteClient(org, identifier, user);
            redirectAttributes.addFlashAttribute("success", "تم إرسال دعوة العميل بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clients";
    }

    @PostMapping("/{id}/cancel-invite")
    public String cancelInvitation(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.cancelInvitation(id);
            redirectAttributes.addFlashAttribute("success", "تم إلغاء دعوة العميل بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clients";
    }

    @PostMapping("/{id}/remove")
    public String removeClient(@PathVariable Long id, @CurrentOrganization Organization org,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.removeClient(id, org);
            redirectAttributes.addFlashAttribute("success", "تم إزالة ارتباط العميل بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/clients";
    }
}
