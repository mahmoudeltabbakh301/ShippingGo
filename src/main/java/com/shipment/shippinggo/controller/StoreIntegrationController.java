package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import com.shipment.shippinggo.service.GovernorateRoutingService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.StoreIntegrationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/store/integrations")
public class StoreIntegrationController {

    private final StoreIntegrationService storeIntegrationService;
    private final GovernorateRoutingService governorateRoutingService;
    private final OrganizationService organizationService;

    public StoreIntegrationController(StoreIntegrationService storeIntegrationService,
            GovernorateRoutingService governorateRoutingService,
            OrganizationService organizationService) {
        this.storeIntegrationService = storeIntegrationService;
        this.governorateRoutingService = governorateRoutingService;
        this.organizationService = organizationService;
    }

    /**
     * الحصول على المنظمة الحالية للمستخدم (Store, Company, أو Office)
     */
    private Organization getCurrentOrganization(User user) {
        return organizationService.getOrganizationByAdmin(user);
    }

    // ==========================================
    // إدارة الربط مع المنصات الخارجية
    // ==========================================

    @GetMapping
    public String showIntegrations(@AuthenticationPrincipal User user, Model model) {
        Organization org = getCurrentOrganization(user);
        if (org == null) {
            return "redirect:/dashboard?error=access_denied";
        }

        // جلب الربطات حسب نوع المنظمة
        List<StoreIntegration> integrations = new ArrayList<>();
        if (org instanceof Store) {
            integrations = storeIntegrationService.getIntegrationsByStore(org.getId());
        } else {
            integrations = storeIntegrationService.getIntegrationsByOrganization(org.getId());
        }

        model.addAttribute("integrations", integrations);
        model.addAttribute("platforms", IntegrationPlatform.values());
        model.addAttribute("isStore", org instanceof Store);

        return "store/integrations";
    }

    @PostMapping("/create")
    public String createIntegration(@AuthenticationPrincipal User user,
            @RequestParam IntegrationPlatform platform,
            @RequestParam(required = false) String externalStoreUrl,
            @RequestParam(required = false) String callbackUrl,
            RedirectAttributes redirectAttributes) {

        Organization org = getCurrentOrganization(user);
        if (org == null) {
            return "redirect:/dashboard?error=access_denied";
        }

        try {
            if (org instanceof Store) {
                storeIntegrationService.createIntegration((Store) org, platform, externalStoreUrl);
            } else {
                storeIntegrationService.createOrganizationIntegration(org, platform, callbackUrl, externalStoreUrl);
            }
            redirectAttributes.addFlashAttribute("success", "تم إضافة منصة " + platform.getDisplayName() + " بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/store/integrations";
    }

    @PostMapping("/{id}/toggle")
    public String toggleIntegration(@AuthenticationPrincipal User user,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            storeIntegrationService.toggleIntegration(id);
            redirectAttributes.addFlashAttribute("success", "تم تغيير حالة الربط بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ أثناء تغيير الحالة: " + e.getMessage());
        }
        return "redirect:/store/integrations";
    }

    @PostMapping("/{id}/delete")
    public String deleteIntegration(@AuthenticationPrincipal User user,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            storeIntegrationService.deleteIntegration(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف منصة الربط بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ أثناء الحذف: " + e.getMessage());
        }
        return "redirect:/store/integrations";
    }

    @PostMapping("/{id}/regenerate-key")
    public String regenerateApiKey(@AuthenticationPrincipal User user,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            storeIntegrationService.regenerateApiKey(id);
            redirectAttributes.addFlashAttribute("success", "تم توليد API Key جديد بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ أثناء توليد المفتاح: " + e.getMessage());
        }
        return "redirect:/store/integrations";
    }

    @PostMapping("/{id}/update-callback")
    public String updateCallbackUrl(@AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) String callbackUrl,
            RedirectAttributes redirectAttributes) {
        try {
            storeIntegrationService.updateCallbackUrl(id, callbackUrl);
            redirectAttributes.addFlashAttribute("success", "تم تحديث رابط الـ Callback بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ: " + e.getMessage());
        }
        return "redirect:/store/integrations";
    }

    // ==========================================
    // قواعد التوزيع التلقائي حسب المحافظة
    // ==========================================

    @GetMapping("/routing")
    public String showRoutingRules(@AuthenticationPrincipal User user, Model model) {
        Organization org = getCurrentOrganization(user);
        if (org == null || !(org instanceof Store)) {
            return "redirect:/dashboard?error=access_denied";
        }

        Store store = (Store) org;

        Governorate[] governorates = Governorate.values();
        List<GovernorateRoutingRule> currentRules = governorateRoutingService.getRulesByStore(store.getId());
        List<Company> joinedCompanies = organizationService.getCompaniesByStore(store.getId());

        model.addAttribute("governorates", governorates);
        model.addAttribute("currentRules", currentRules);
        model.addAttribute("joinedCompanies", joinedCompanies);

        return "store/routing-rules";
    }

    @PostMapping("/routing/save")
    public String saveRoutingRule(@AuthenticationPrincipal User user,
            @RequestParam Governorate governorate,
            @RequestParam Long targetOrganizationId,
            RedirectAttributes redirectAttributes) {

        Organization org = getCurrentOrganization(user);
        if (org == null || !(org instanceof Store)) {
            return "redirect:/dashboard?error=access_denied";
        }

        Store store = (Store) org;

        try {
            governorateRoutingService.saveRule(store, governorate, targetOrganizationId);
            redirectAttributes.addFlashAttribute("success", "تم حفظ قاعدة التوزيع لمحافظة " + governorate.getArabicName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ: " + e.getMessage());
        }

        return "redirect:/store/integrations/routing";
    }

    @PostMapping("/routing/{id}/delete")
    public String deleteRoutingRule(@AuthenticationPrincipal User user,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            governorateRoutingService.deleteRule(id);
            redirectAttributes.addFlashAttribute("success", "تم إزالة قاعدة التوزيع بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "حدث خطأ: " + e.getMessage());
        }

        return "redirect:/store/integrations/routing";
    }
}
