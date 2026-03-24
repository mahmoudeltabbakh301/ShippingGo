package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/couriers")
public class ApiCourierTrackingController {

    private final OrganizationService organizationService;

    public ApiCourierTrackingController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * GET /api/couriers/locations
     * Returns the live locations of all couriers in the authenticated user's organization.
     * Used by the web map page to auto-refresh marker positions.
     */
    @GetMapping("/locations")
    public ResponseEntity<?> getCourierLocations(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        // Only ADMIN, MANAGER, SUPER_ADMIN, and DATA_ENTRY can view courier locations
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.SUPER_ADMIN && user.getRole() != Role.DATA_ENTRY) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Access denied"));
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return ResponseEntity.ok(Map.of("success", true, "data", List.of()));
        }

        List<User> couriers = organizationService.getCouriers(org);
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> courierData = couriers.stream().map(courier -> {
            Map<String, Object> data = new HashMap<>();
            data.put("id", courier.getId());
            data.put("fullName", courier.getFullName());
            data.put("phone", courier.getPhone());
            data.put("lastLatitude", courier.getLastLatitude());
            data.put("lastLongitude", courier.getLastLongitude());
            data.put("lastLocationUpdateTime",
                    courier.getLastLocationUpdateTime() != null ? courier.getLastLocationUpdateTime().toString() : null);

            // Consider "online" if location was updated within the last 5 minutes
            boolean isOnline = courier.getLastLocationUpdateTime() != null
                    && ChronoUnit.MINUTES.between(courier.getLastLocationUpdateTime(), now) <= 5;
            data.put("online", isOnline);

            return data;
        }).toList();

        return ResponseEntity.ok(Map.of("success", true, "data", courierData));
    }
}
