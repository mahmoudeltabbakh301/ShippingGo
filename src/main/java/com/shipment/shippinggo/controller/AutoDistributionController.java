package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.AutoDistributionService;
import com.shipment.shippinggo.service.GovernorateZoneService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * التوزيع التلقائي — إسناد الأوردرات المختارة للمناديب بناءً على المنطقة.
 */
@Controller
@RequestMapping("/auto-distribution")
public class AutoDistributionController {

    private final AutoDistributionService autoDistributionService;
    private final OrganizationService organizationService;
    private final GovernorateZoneService governorateZoneService;

    public AutoDistributionController(AutoDistributionService autoDistributionService,
                                      OrganizationService organizationService,
                                      GovernorateZoneService governorateZoneService) {
        this.autoDistributionService = autoDistributionService;
        this.organizationService = organizationService;
        this.governorateZoneService = governorateZoneService;
    }

    @GetMapping
    public String index(@CurrentOrganization Organization org,
                       @AuthenticationPrincipal User user,
                       Model model) {
        if (org == null) return "redirect:/members/invitations";

        List<CourierZoneAssignment> zoneAssignments = autoDistributionService.getZoneAssignments(org.getId());
        List<User> couriers = organizationService.getCouriers(org);

        // تجميع المناطق حسب المندوب
        java.util.Map<User, List<CourierZoneAssignment>> groupedByCourier = new java.util.LinkedHashMap<>();
        for (CourierZoneAssignment za : zoneAssignments) {
            groupedByCourier.computeIfAbsent(za.getCourier(), k -> new java.util.ArrayList<>()).add(za);
        }

        model.addAttribute("organization", org);
        model.addAttribute("zoneAssignments", zoneAssignments);
        model.addAttribute("groupedByCourier", groupedByCourier);
        model.addAttribute("couriers", couriers);
        model.addAttribute("courierDisplayNames", organizationService.buildCourierDisplayNameMap(org));
        model.addAttribute("governorates", Governorate.values());
        return "auto-distribution/index";
    }

    /**
     * صفحة تفاصيل المناطق المربوطة بمندوب معين.
     */
    @GetMapping("/courier/{courierId}")
    public String courierZoneDetails(@CurrentOrganization Organization org,
                                     @PathVariable Long courierId,
                                     Model model) {
        if (org == null) return "redirect:/members/invitations";

        User courier = organizationService.getUserById(courierId);
        if (courier == null) return "redirect:/auto-distribution";

        List<CourierZoneAssignment> courierZones = autoDistributionService.getCourierZones(courierId, org.getId());

        // تجميع المناطق حسب المحافظة
        java.util.Map<Governorate, List<CourierZoneAssignment>> groupedByGov = new java.util.LinkedHashMap<>();
        for (CourierZoneAssignment zone : courierZones) {
            groupedByGov.computeIfAbsent(zone.getGovernorate(), k -> new java.util.ArrayList<>()).add(zone);
        }

        model.addAttribute("organization", org);
        model.addAttribute("courier", courier);
        String courierDisplayName = organizationService.getCourierDisplayName(courier, org);
        model.addAttribute("courierDisplayName", courierDisplayName);
        model.addAttribute("pageTitle", "مناطق المندوب: " + courierDisplayName);
        model.addAttribute("courierZones", courierZones);
        model.addAttribute("groupedByGov", groupedByGov);
        return "auto-distribution/courier-details";
    }

    @PostMapping("/assign")
    public String assignCourierToZone(@CurrentOrganization Organization org,
                                     @RequestParam Long courierId,
                                     @RequestParam Governorate governorate,
                                     @RequestParam(required = false) String district,
                                     @RequestParam(required = false) String center,
                                     @RequestParam(required = false) String area,
                                     RedirectAttributes redirectAttributes) {
        try {
            User courier = organizationService.getUserById(courierId);
            if (courier == null) throw new RuntimeException("المندوب غير موجود");

            String districtValue = (district != null && !district.trim().isEmpty()) ? district.trim() : null;
            String centerValue = (center != null && !center.trim().isEmpty()) ? center.trim() : null;
            String areaValue = (area != null && !area.trim().isEmpty()) ? area.trim() : null;
            autoDistributionService.assignCourierToZone(courier, org, governorate, districtValue, centerValue, areaValue);
            redirectAttributes.addFlashAttribute("success", "تم ربط المندوب بالمنطقة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auto-distribution";
    }

    @PostMapping("/remove/{id}")
    public String removeZoneAssignment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            autoDistributionService.removeZoneAssignment(id);
            redirectAttributes.addFlashAttribute("success", "تم إزالة الربط");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auto-distribution";
    }

    @PostMapping("/execute")
    public String executeDistribution(@CurrentOrganization Organization org,
                                     @AuthenticationPrincipal User user,
                                     @RequestParam(value = "selectedIds") List<Long> orderIds,
                                     RedirectAttributes redirectAttributes) {
        try {
            AutoDistributionService.DistributionResult result =
                    autoDistributionService.autoDistribute(orderIds, org.getId(), user);
            redirectAttributes.addFlashAttribute("success",
                    String.format("تم توزيع %d أوردر بنجاح. تم تخطي %d أوردر.",
                            result.getAssignedCount(), result.getSkippedCount()));
            redirectAttributes.addFlashAttribute("distributionResult", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auto-distribution";
    }

    /**
     * التوزيع التلقائي من صفحة يوم العمل — يعيد التوجيه ليوم العمل بعد التنفيذ.
     */
    @PostMapping("/execute-from-day")
    public String executeFromDay(@CurrentOrganization Organization org,
                                 @AuthenticationPrincipal User user,
                                 @RequestParam(value = "selectedIds") String selectedIdsStr,
                                 @RequestParam Long businessDayId,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (selectedIdsStr == null || selectedIdsStr.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "يرجى تحديد طلبات أولاً");
                return "redirect:/business-days/" + businessDayId;
            }

            List<Long> orderIds = java.util.Arrays.stream(selectedIdsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(java.util.stream.Collectors.toList());

            if (orderIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "لا توجد طلبات محددة");
                return "redirect:/business-days/" + businessDayId;
            }

            AutoDistributionService.DistributionResult result =
                    autoDistributionService.autoDistribute(orderIds, org.getId(), user);
            redirectAttributes.addFlashAttribute("success",
                    String.format("التوزيع التلقائي: تم إسناد %d أوردر. تم تخطي %d أوردر.",
                            result.getAssignedCount(), result.getSkippedCount()));
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "خطأ في تنسيق الطلبات المحددة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ في التوزيع: " + e.getMessage());
        }
        return "redirect:/business-days/" + businessDayId;
    }
}
