package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.report.*;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ApiReportController {

    private final ReportingService reportingService;
    private final OrganizationService organizationService;

    @GetMapping("/period")
    public ResponseEntity<PeriodReport> getPeriodReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null)
            return ResponseEntity.badRequest().build();

        // Defaults to last 30 days if not provided
        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(reportingService.getPeriodReport(org.getId(), from, to));
    }

    @GetMapping("/trends")
    public ResponseEntity<TrendData> getTrendsReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null)
            return ResponseEntity.badRequest().build();

        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(reportingService.getTrendsReport(org.getId(), from, to));
    }

    @GetMapping("/courier/{courierId}")
    public ResponseEntity<CourierReport> getCourierReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long courierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(reportingService.getCourierReport(courierId, from, to));
    }

    @GetMapping("/organization/{targetOrgId}")
    public ResponseEntity<OrganizationReport> getOrganizationReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long targetOrgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(reportingService.getOrganizationReport(targetOrgId, from, to));
    }

    @GetMapping("/geographic")
    public ResponseEntity<GeographicReport> getGeographicReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null)
            return ResponseEntity.badRequest().build();

        if (from == null)
            from = LocalDate.now().minusDays(30);
        if (to == null)
            to = LocalDate.now();

        return ResponseEntity.ok(reportingService.getGeographicReport(org.getId(), from, to));
    }
}
