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

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @org.springframework.web.bind.annotation.RequestParam("username") String username,
            @org.springframework.web.bind.annotation.RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        
        try {
            boolean success = userService.initiatePasswordReset(username, email);
            if (success) {
                redirectAttributes.addAttribute("username", username);
                redirectAttributes.addFlashAttribute("success", "تم إرسال كود استعادة كلمة المرور إلى بريدك الإلكتروني.");
                return "redirect:/reset-password";
            } else {
                redirectAttributes.addFlashAttribute("error", "اسم المستخدم أو البريد الإلكتروني غير صحيح.");
                return "redirect:/forgot-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ أثناء معالجة الطلب، حاول مرة أخرى.");
            return "redirect:/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @org.springframework.web.bind.annotation.RequestParam(value = "username", required = false) String username,
            Model model) {
        if (isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (username == null || username.trim().isEmpty()) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("username", username);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @org.springframework.web.bind.annotation.RequestParam("username") String username,
            @org.springframework.web.bind.annotation.RequestParam("code") String code,
            @org.springframework.web.bind.annotation.RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {
        
        try {
            boolean success = userService.verifyAndResetPassword(username, code, newPassword);
            if (success) {
                redirectAttributes.addFlashAttribute("success", "تم تغيير كلمة المرور بنجاح. يمكنك الآن تسجيل الدخول.");
                return "redirect:/login";
            } else {
                redirectAttributes.addAttribute("username", username);
                redirectAttributes.addFlashAttribute("error", "الكود غير صحيح أو منتهي الصلاحية.");
                return "redirect:/reset-password";
            }
        } catch (RuntimeException e) {
            redirectAttributes.addAttribute("username", username);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reset-password";
        }
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
