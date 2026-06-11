package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.service.PlatformSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/pages")
public class StaticPageController {

    private final PlatformSettingService platformSettingService;

    public StaticPageController(PlatformSettingService platformSettingService) {
        this.platformSettingService = platformSettingService;
    }

    @GetMapping("/terms")
    public String termsAndConditions(Model model) {
        model.addAttribute("content", platformSettingService.getSetting("terms_and_conditions", ""));
        model.addAttribute("platformName", platformSettingService.getSetting("platform_name", "ShippingGo"));
        model.addAttribute("pageTitle", "الشروط والأحكام");
        return "pages/terms";
    }

    @GetMapping("/privacy")
    public String privacyPolicy(Model model) {
        model.addAttribute("content", platformSettingService.getSetting("privacy_policy", ""));
        model.addAttribute("platformName", platformSettingService.getSetting("platform_name", "ShippingGo"));
        model.addAttribute("pageTitle", "سياسة الخصوصية");
        return "pages/privacy";
    }
}
