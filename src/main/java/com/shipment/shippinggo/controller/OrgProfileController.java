package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/org")
public class OrgProfileController {

    private final OrganizationService organizationService;

    public OrgProfileController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Organization org = organizationService.getById(id);
        if (org == null) {
            return "redirect:/"; // Or a specific 404 page
        }

        model.addAttribute("organization", org);
        return "org/profile";
    }
}
