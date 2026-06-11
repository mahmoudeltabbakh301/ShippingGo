package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.service.ShippingPriceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * إدارة قوائم أسعار الشحن — /settings/pricing
 */
@Controller
@RequestMapping("/settings/pricing")
public class ShippingPriceController {

    private final ShippingPriceService priceService;

    public ShippingPriceController(ShippingPriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public String pricingPage(@CurrentOrganization Organization org, Model model) {
        if (org == null) return "redirect:/members/invitations";

        List<ShippingPriceList> priceLists = priceService.getPriceLists(org.getId());
        model.addAttribute("organization", org);
        model.addAttribute("priceLists", priceLists);
        model.addAttribute("governorates", Governorate.values());
        return "settings/pricing";
    }

    @PostMapping("/list/create")
    public String createPriceList(@CurrentOrganization Organization org,
                                  @RequestParam String name,
                                  @RequestParam(defaultValue = "false") boolean isDefault,
                                  RedirectAttributes redirectAttributes) {
        try {
            priceService.createPriceList(org, name, isDefault);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء قائمة الأسعار بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/pricing";
    }

    @PostMapping("/list/{id}/update")
    public String updatePriceList(@PathVariable Long id,
                                  @RequestParam String name,
                                  @RequestParam(defaultValue = "false") boolean isDefault,
                                  @RequestParam(defaultValue = "true") boolean active,
                                  RedirectAttributes redirectAttributes) {
        try {
            priceService.updatePriceList(id, name, isDefault, active);
            redirectAttributes.addFlashAttribute("success", "تم تحديث قائمة الأسعار");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/pricing";
    }

    @PostMapping("/list/{id}/delete")
    public String deletePriceList(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            priceService.deletePriceList(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف قائمة الأسعار");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/pricing";
    }

    @GetMapping("/list/{id}")
    public String priceListDetail(@PathVariable Long id,
                                  @CurrentOrganization Organization org,
                                  Model model) {
        if (org == null) return "redirect:/members/invitations";

        ShippingPriceList priceList = priceService.getPriceListById(id)
                .orElseThrow(() -> new RuntimeException("قائمة الأسعار غير موجودة"));

        List<ShippingPriceEntry> entries = priceService.getEntries(id);
        model.addAttribute("organization", org);
        model.addAttribute("priceList", priceList);
        model.addAttribute("entries", entries);
        model.addAttribute("governorates", Governorate.values());
        return "settings/pricing-detail";
    }

    @PostMapping("/entry/save")
    public String saveEntry(@RequestParam Long priceListId,
                           @RequestParam Governorate governorate,
                           @RequestParam BigDecimal shippingPrice,
                           @RequestParam(required = false) BigDecimal returnPrice,
                           RedirectAttributes redirectAttributes) {
        try {
            priceService.saveEntry(priceListId, governorate, shippingPrice, returnPrice);
            redirectAttributes.addFlashAttribute("success", "تم حفظ السعر بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/pricing/list/" + priceListId;
    }

    @PostMapping("/entry/{id}/delete")
    public String deleteEntry(@PathVariable Long id,
                             @RequestParam Long priceListId,
                             RedirectAttributes redirectAttributes) {
        try {
            priceService.deleteEntry(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف السعر");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/pricing/list/" + priceListId;
    }
}
