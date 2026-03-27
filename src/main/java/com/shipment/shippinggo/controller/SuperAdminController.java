package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.SuperAdminService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final OrganizationService organizationService;

    public SuperAdminController(SuperAdminService superAdminService,
                                 OrganizationService organizationService) {
        this.superAdminService = superAdminService;
        this.organizationService = organizationService;
    }

    // ===== Dashboard =====

    @GetMapping
    public String dashboard(Model model) {
        SuperAdminService.PlatformStats stats = superAdminService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("organizations", superAdminService.getLatestOrganizations(5));
        model.addAttribute("pageTitle", "لوحة تحكم المدير العام");

        return "super-admin/dashboard";
    }

    // ===== Organizations =====

    @GetMapping("/organizations")
    public String listOrganizations(@RequestParam(required = false) String search, Model model) {
        java.util.List<Organization> allOrgs = superAdminService.getAllOrganizations();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            allOrgs = allOrgs.stream()
                    .filter(o -> o.getName().toLowerCase().contains(q)
                            || (o.getPhone() != null && o.getPhone().contains(q))
                            || (o.getEmail() != null && o.getEmail().toLowerCase().contains(q)))
                    .toList();
        }

        model.addAttribute("organizations", allOrgs);
        model.addAttribute("search", search);
        model.addAttribute("pageTitle", "إدارة المنظمات");
        return "super-admin/organizations";
    }

    @GetMapping("/organizations/{id}")
    public String organizationDetail(@PathVariable Long id, Model model) {
        Organization org = superAdminService.getOrganizationById(id);
        model.addAttribute("org", org);
        model.addAttribute("admin", org.getAdmin());
        model.addAttribute("members", organizationService.getAcceptedMemberships(id));
        model.addAttribute("pendingMembers", organizationService.getPendingMemberships(id));
        model.addAttribute("pageTitle", "تفاصيل المنظمة - " + org.getName());
        return "super-admin/organization-detail";
    }

    @PostMapping("/organizations/{id}/toggle")
    public String toggleOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.toggleOrganizationActive(id);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations";
    }

    // ===== Users =====

    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String search, Model model) {
        java.util.List<User> users = superAdminService.getAllUsers();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(q)
                            || u.getUsername().toLowerCase().contains(q)
                            || u.getEmail().toLowerCase().contains(q)
                            || (u.getPhone() != null && u.getPhone().contains(q)))
                    .toList();
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "إدارة المستخدمين");
        return "super-admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = superAdminService.getUserById(id);
        Organization userOrg = organizationService.getOrganizationByUser(user);
        model.addAttribute("targetUser", user);
        model.addAttribute("userOrg", userOrg);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "تفاصيل المستخدم - " + user.getFullName());
        return "super-admin/user-detail";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.toggleUserEnabled(id);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة المستخدم بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id,
                                 @RequestParam Role role,
                                 RedirectAttributes redirectAttributes) {
        try {
            superAdminService.changeUserRole(id, role);
            redirectAttributes.addFlashAttribute("success", "تم تغيير صلاحية المستخدم بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users/" + id;
    }
}
