package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.dto.report.*;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
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

    // =====================================================================
    // 1. تقرير الفترة (Period Report)
    // =====================================================================

    @GetMapping("/period")
    public ResponseEntity<PeriodReport> getPeriodReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long businessDayId) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (businessDayId != null) {
            return ResponseEntity.ok(reportingService.getPeriodReportByBusinessDay(org.getId(), businessDayId));
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getPeriodReport(org.getId(), from, to));
    }

    // =====================================================================
    // 2. التقرير المالي الشامل (Financial Summary)
    // =====================================================================

    @GetMapping("/financial")
    public ResponseEntity<FinancialSummaryReport> getFinancialSummary(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (businessDayId != null) {
            return ResponseEntity.ok(reportingService.getFinancialSummaryByBusinessDay(org, businessDayId));
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getFinancialSummaryByPeriod(org, from, to));
    }

    // =====================================================================
    // 3. تقرير المندوب (Courier Report)
    // =====================================================================

    @GetMapping("/courier/{courierId}")
    public ResponseEntity<CourierReport> getCourierReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long courierId,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (businessDayId != null) {
            return ResponseEntity.ok(reportingService.getCourierReportByBusinessDay(org, courierId, businessDayId));
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getCourierReport(org, courierId, from, to));
    }

    // =====================================================================
    // 4. تقرير المنظمة (Organization Report)
    // =====================================================================

    @GetMapping("/organization/{targetOrgId}")
    public ResponseEntity<OrganizationReport> getOrganizationReport(
            @AuthenticationPrincipal User user,
            @PathVariable Long targetOrgId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        String dir = direction != null ? direction : "OUTGOING";

        if (businessDayId != null) {
            return ResponseEntity.ok(reportingService.getOrganizationReportByBusinessDay(org, targetOrgId, dir, businessDayId));
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getOrganizationReport(org, targetOrgId, dir, from, to));
    }

    // =====================================================================
    // 5. مقارنة أداء المناديب
    // =====================================================================

    @GetMapping("/performance/couriers")
    public ResponseEntity<PerformanceComparison> getCourierPerformance(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(reportingService.getCourierPerformanceComparison(org, businessDayId));
    }

    // =====================================================================
    // 6. مقارنة أداء المنظمات
    // =====================================================================

    @GetMapping("/performance/organizations")
    public ResponseEntity<PerformanceComparison> getOrganizationPerformance(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(reportingService.getOrganizationPerformanceComparison(org, businessDayId));
    }

    // =====================================================================
    // 7. التقرير الجغرافي
    // =====================================================================

    @GetMapping("/geographic")
    public ResponseEntity<GeographicReport> getGeographicReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (businessDayId != null) {
            return ResponseEntity.ok(reportingService.getGeographicReportByBusinessDay(org.getId(), businessDayId));
        }

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getGeographicReport(org.getId(), from, to));
    }

    // =====================================================================
    // 8. تقرير الاتجاهات
    // =====================================================================

    @GetMapping("/trends")
    public ResponseEntity<TrendData> getTrendsReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportingService.getTrendsReport(org.getId(), from, to));
    }
}
