package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.*;
import com.shipment.shippinggo.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;
    private final VehicleService vehicleService;
    private final OrganizationService organizationService;
    private final QrCodeService qrCodeService;

    public TripController(TripService tripService, VehicleService vehicleService,
                          OrganizationService organizationService, QrCodeService qrCodeService) {
        this.tripService = tripService;
        this.vehicleService = vehicleService;
        this.organizationService = organizationService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * عرض قائمة الرحلات
     */
    @GetMapping
    public String listTrips(@RequestParam(required = false, defaultValue = "all") String tab,
                            @RequestParam(required = false) TripStatus status,
                            @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                            Model model) {
        if (org.getType() == OrganizationType.CLIENT) {
            return "redirect:/dashboard";
        }

        List<Trip> allTrips = tripService.getTripsByOrganization(org.getId());
        List<Trip> filteredTrips;

        // Filter by tab
        if ("incoming".equals(tab)) {
            filteredTrips = allTrips.stream()
                    .filter(t -> t.getDestinationOrganization() != null && t.getDestinationOrganization().getId().equals(org.getId()) && (t.getStatus() == TripStatus.IN_TRANSIT || t.getStatus() == TripStatus.ARRIVED))
                    .toList();
        } else if ("outgoing".equals(tab)) {
            filteredTrips = allTrips.stream()
                    .filter(t -> t.getOriginOrganization().getId().equals(org.getId()) && t.getStatus() != TripStatus.RETURNING && t.getStatus() != TripStatus.RETURNED)
                    .toList();
        } else if ("incoming_returns".equals(tab)) {
            filteredTrips = allTrips.stream()
                    .filter(t -> t.getOriginOrganization().getId().equals(org.getId()) && (t.getStatus() == TripStatus.RETURNING || t.getStatus() == TripStatus.RETURNED))
                    .toList();
        } else if ("outgoing_returns".equals(tab)) {
            filteredTrips = allTrips.stream()
                    .filter(t -> t.getDestinationOrganization() != null && t.getDestinationOrganization().getId().equals(org.getId()) && (t.getStatus() == TripStatus.RETURNING || t.getStatus() == TripStatus.RETURNED))
                    .toList();
        } else {
            filteredTrips = allTrips;
        }

        if (status != null) {
            filteredTrips = filteredTrips.stream().filter(t -> t.getStatus() == status).toList();
        }

        model.addAttribute("trips", filteredTrips);
        model.addAttribute("organization", org);
        model.addAttribute("statuses", TripStatus.values());
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentTab", tab);
        model.addAttribute("vehicles", vehicleService.getVehiclesByOrganization(org.getId()));

        return "trips/list";
    }

    /**
     * عرض صفحة إنشاء رحلة جديدة
     */
    @GetMapping("/new")
    public String newTripForm(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                              Model model) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            return "redirect:/trips";
        }

        model.addAttribute("organization", org);
        model.addAttribute("vehicles", vehicleService.getAvailableVehicles(org.getId()));
        model.addAttribute("linkedOrganizations", organizationService.getLinkedOrganizations(org));

        return "trips/form";
    }

    /**
     * إنشاء رحلة جديدة
     */
    @PostMapping("/new")
    public String createTrip(@RequestParam Long vehicleId,
                             @RequestParam(required = false) Long destinationOrgId,
                             @RequestParam(required = false) Long businessDayId,
                             @RequestParam(required = false) String notes,
                             @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                             RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح");
            return "redirect:/trips";
        }

        try {
            Trip trip = tripService.createTrip(vehicleId, org, destinationOrgId, businessDayId, notes, user);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء الرحلة بنجاح — كود الرحلة: " + trip.getCode());
            return "redirect:/trips/" + trip.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/trips/new";
        }
    }

    /**
     * عرض تفاصيل رحلة
     */
    @GetMapping("/{id}")
    public String viewTrip(@PathVariable Long id,
                           @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                           Model model) {
        Trip trip = tripService.getById(id);
        if (trip == null) {
            return "redirect:/trips";
        }

        // التحقق من أن المنظمة هي المرسل أو المستلم
        boolean isOrigin = trip.getOriginOrganization().getId().equals(org.getId());
        boolean isDestination = trip.getDestinationOrganization() != null &&
                trip.getDestinationOrganization().getId().equals(org.getId());

        if (!isOrigin && !isDestination) {
            return "redirect:/trips";
        }

        List<TripOrder> tripOrders = tripService.getTripOrders(id);

        model.addAttribute("trip", trip);
        model.addAttribute("tripOrders", tripOrders);
        model.addAttribute("organization", org);
        model.addAttribute("isOrigin", isOrigin);
        model.addAttribute("isDestination", isDestination);

        return "trips/view";
    }

    /**
     * إضافة أوردرات للرحلة
     */
    @PostMapping("/{id}/add-orders")
    public String addOrdersToTrip(@PathVariable Long id,
                                   @RequestParam String orderIds,
                                   @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {
        try {
            List<Long> ids = parseOrderIds(orderIds);
            tripService.addOrdersToTrip(id, ids, org.getId());
            redirectAttributes.addFlashAttribute("success", "تم إضافة " + ids.size() + " طلب للرحلة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trips/" + id;
    }

    /**
     * إزالة أوردر من الرحلة
     */
    @PostMapping("/{id}/remove-order/{orderId}")
    public String removeOrderFromTrip(@PathVariable Long id, @PathVariable Long orderId,
                                       @CurrentOrganization Organization org,
                                       RedirectAttributes redirectAttributes) {
        try {
            tripService.removeOrderFromTrip(id, orderId, org.getId());
            redirectAttributes.addFlashAttribute("success", "تم إزالة الطلب من الرحلة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trips/" + id;
    }

    /**
     * تحديث حالة الرحلة
     */
    @PostMapping("/{id}/status")
    public String updateTripStatus(@PathVariable Long id,
                                    @RequestParam TripStatus status,
                                    @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        try {
            tripService.updateTripStatus(id, status, org.getId());
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة الرحلة إلى: " + status.getArabicName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trips/" + id;
    }

    /**
     * إسناد أوردرات لشاحنة من يوم العمل (Bulk Assign)
     */
    @PostMapping("/assign-from-day")
    public String assignFromBusinessDay(@RequestParam String orderIds,
                                        @RequestParam Long vehicleId,
                                        @RequestParam Long businessDayId,
                                        @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                        RedirectAttributes redirectAttributes) {
        try {
            List<Long> ids = parseOrderIds(orderIds);
            Trip trip = tripService.assignOrdersToVehicle(ids, vehicleId, org, businessDayId, user);
            redirectAttributes.addFlashAttribute("success",
                    "تم إسناد " + ids.size() + " طلب للشاحنة — كود الرحلة: " + trip.getCode());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/business-days/" + businessDayId;
    }

    /**
     * إلغاء الرحلة
     */
    @PostMapping("/{id}/cancel")
    public String cancelTrip(@PathVariable Long id,
                             @CurrentOrganization Organization org,
                             RedirectAttributes redirectAttributes) {
        try {
            tripService.cancelTrip(id, org.getId());
            redirectAttributes.addFlashAttribute("success", "تم إلغاء الرحلة بنجاح وتفريغها من الطلبات وإعادة الشاحنة للعمل.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/trips/" + id;
    }

    // ======= Helper =======

    private List<Long> parseOrderIds(String orderIds) {
        if (orderIds == null || orderIds.trim().isEmpty()) {
            return List.of();
        }
        return java.util.Arrays.stream(orderIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
