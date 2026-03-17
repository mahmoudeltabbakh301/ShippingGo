package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;

    public SettingsController(UserService userService, OrganizationService organizationService,
            PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.organizationService = organizationService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String showSettings(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);

        Organization org = organizationService.getOrganizationByUser(user);
        if (org != null) {
            model.addAttribute("organization", org);
        }

        return "settings";
    }

    @GetMapping("/personal")
    public String showPersonalSettings(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);

        Organization org = organizationService.getOrganizationByUser(user);
        if (org != null) {
            model.addAttribute("organization", org);
        }

        return "personal-settings";
    }

    @PostMapping("/update-password")
    public String updatePassword(@AuthenticationPrincipal User user,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        try {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new RuntimeException("كلمة المرور الحالية غير صحيحة");
            }
            if (!newPassword.equals(confirmPassword)) {
                throw new RuntimeException("كلمة المرور الجديدة غير متطابقة");
            }
            if (newPassword.length() < 6) {
                throw new RuntimeException("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            }

            userService.updatePassword(user, newPassword);
            redirectAttributes.addFlashAttribute("success", "تم تغيير كلمة المرور بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/personal";
    }

    @PostMapping("/update-organization")
    public String updateOrganization(@AuthenticationPrincipal User user,
            @RequestParam String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String about,
            RedirectAttributes redirectAttributes) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                throw new RuntimeException("لا توجد منظمة مرتبطة بحسابك");
            }

            org.setName(name);
            org.setAddress(address);
            org.setPhone(phone);
            org.setEmail(email);
            org.setAbout(about);

            organizationService.saveOrganization(org);
            redirectAttributes.addFlashAttribute("success", "تم تحديث بيانات المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/personal";
    }
}
