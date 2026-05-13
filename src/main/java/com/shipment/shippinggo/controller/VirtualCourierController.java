package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Controller
@RequestMapping("/virtual-couriers")
public class VirtualCourierController {

    @Autowired
    private OrganizationService organizationService;

    @GetMapping
    public String listVirtualCouriers(Model model, @AuthenticationPrincipal User currentUser) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        List<User> virtualCouriers = organizationService.getVirtualCouriers(currentOrg);
        model.addAttribute("virtualCouriers", virtualCouriers);
        model.addAttribute("currentOrg", currentOrg);

        return "virtual-couriers/list";
    }

    @PostMapping("/create")
    @Transactional
    public String createVirtualCourier(@RequestParam String name,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        try {
            organizationService.createVirtualCourier(currentOrg, name, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "تم إنشاء المندوب الافتراضي بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/virtual-couriers";
    }

    @PostMapping("/update/{id}")
    @Transactional
    public String updateVirtualCourier(@PathVariable Long id,
            @RequestParam String name,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        try {
            organizationService.updateVirtualCourier(id, name, currentOrg);
            redirectAttributes.addFlashAttribute("successMessage", "تم تحديث المندوب الافتراضي بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/virtual-couriers";
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String deleteVirtualCourier(@PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        try {
            organizationService.deleteVirtualCourier(id, currentOrg);
            redirectAttributes.addFlashAttribute("successMessage", "تم حذف المندوب الافتراضي بنجاح. البيانات التاريخية محفوظة.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/virtual-couriers";
    }
}
