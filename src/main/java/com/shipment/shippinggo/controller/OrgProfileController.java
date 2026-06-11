package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.ShippingPriceEntry;
import com.shipment.shippinggo.entity.ShippingPriceList;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.ShippingPriceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/org")
public class OrgProfileController {

    private final OrganizationService organizationService;
    private final ShippingPriceService shippingPriceService;

    public OrgProfileController(OrganizationService organizationService,
                                ShippingPriceService shippingPriceService) {
        this.organizationService = organizationService;
        this.shippingPriceService = shippingPriceService;
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Organization org = organizationService.getById(id);
        if (org == null) {
            return "redirect:/";
        }

        model.addAttribute("organization", org);

        // تحميل جميع قوائم الأسعار النشطة مع أسعارها للعرض في البروفايل العام
        try {
            List<ShippingPriceList> priceLists = shippingPriceService.getActivePriceLists(org.getId());

            // بناء خريطة: كل قائمة أسعار -> أسعار المحافظات الخاصة بها
            Map<ShippingPriceList, List<ShippingPriceEntry>> priceListsWithEntries = new LinkedHashMap<>();
            for (ShippingPriceList pl : priceLists) {
                List<ShippingPriceEntry> entries = shippingPriceService.getEntriesByPriceList(pl.getId());
                priceListsWithEntries.put(pl, entries);
            }

            model.addAttribute("priceListsWithEntries", priceListsWithEntries);
            model.addAttribute("priceLists", priceLists);
        } catch (Exception e) {
            model.addAttribute("priceListsWithEntries", Collections.emptyMap());
            model.addAttribute("priceLists", Collections.emptyList());
        }

        return "org/profile";
    }
}
