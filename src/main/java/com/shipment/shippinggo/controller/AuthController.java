package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.dto.RegistrationDto;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final OrganizationService organizationService;

    public AuthController(UserService userService, OrganizationService organizationService) {
        this.userService = userService;
        this.organizationService = organizationService;
    }

    private boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated()
                && !(auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()));
    }

    @GetMapping("/")
    public String home() {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registrationDto", new RegistrationDto());
        model.addAttribute("organizationTypes", OrganizationType.values());
        model.addAttribute("governorates", com.shipment.shippinggo.enums.Governorate.values());
        model.addAttribute("companies", organizationService.getAllCompanies());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegistrationDto registrationDto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("organizationTypes", OrganizationType.values());
            model.addAttribute("governorates", com.shipment.shippinggo.enums.Governorate.values());
            model.addAttribute("companies", organizationService.getAllCompanies());
            return "auth/register";
        }

        try {
            userService.registerUser(registrationDto);
            redirectAttributes.addAttribute("email", registrationDto.getEmail());
            redirectAttributes.addFlashAttribute("success", "تم إنشاء الحساب بنجاح. يرجى مراجعة بريدك الإلكتروني لمعرفة كود التفعيل.");
            return "redirect:/verify";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("organizationTypes", OrganizationType.values());
            model.addAttribute("governorates", com.shipment.shippinggo.enums.Governorate.values());
            model.addAttribute("companies", organizationService.getAllCompanies());
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String showVerifyForm(@org.springframework.web.bind.annotation.RequestParam(value = "email", required = false) String email, Model model) {
        if (email == null) {
            return "redirect:/login";
        }
        model.addAttribute("email", email);
        return "auth/verify";
    }

    @PostMapping("/verify")
    public String verifyUser(@org.springframework.web.bind.annotation.RequestParam("email") String email, 
                             @org.springframework.web.bind.annotation.RequestParam("code") String code, 
                             RedirectAttributes redirectAttributes) {
        boolean isVerified = userService.verifyUser(email, code);
        if (isVerified) {
            redirectAttributes.addFlashAttribute("success", "تم تفعيل حسابك بنجاح. يمكنك الآن تسجيل الدخول.");
            return "redirect:/login";
        } else {
            redirectAttributes.addAttribute("email", email);
            redirectAttributes.addFlashAttribute("error", "كود التفعيل غير صالح أو تم تفعيل الحساب مسبقاً.");
            return "redirect:/verify";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
