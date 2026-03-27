package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/business-days")
public class ApiBusinessDayController {

    private final BusinessDayService businessDayService;
    private final OrganizationService organizationService;

    public ApiBusinessDayController(BusinessDayService businessDayService, OrganizationService organizationService) {
        this.businessDayService = businessDayService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessDay>>> listBusinessDays(@AuthenticationPrincipal User user) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("User does not belong to any organization"));
        }

        List<BusinessDay> days = businessDayService.getBusinessDaysForUser(org.getId(), user);
        return ResponseEntity.ok(ApiResponse.success(days));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessDay>> createBusinessDay(
            @RequestParam String dateStr,
            @AuthenticationPrincipal User user) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("User does not belong to any organization"));
            }

            BusinessDay day = businessDayService.createBusinessDay(org.getId(), java.time.LocalDate.parse(dateStr),
                    null, user);
            return ResponseEntity.ok(ApiResponse.success(day, "Business day created"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBusinessDay(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        try {
            businessDayService.deleteBusinessDay(id, user); // Assuming the service checks if delete is safe/allowed
            return ResponseEntity.ok(ApiResponse.success(null, "Business day deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
