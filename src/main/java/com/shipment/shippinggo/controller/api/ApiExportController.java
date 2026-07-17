package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.report.PerformanceComparison;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.ExcelExportService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * API لتصدير التقارير بصيغة Excel
 */
@RestController
@RequestMapping("/api/reports/export")
@RequiredArgsConstructor
public class ApiExportController {

    private final ExcelExportService excelExportService;
    private final OrganizationService organizationService;
    private final ReportingService reportingService;

    @GetMapping("/period")
    public ResponseEntity<byte[]> exportPeriodReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            byte[] bytes = excelExportService.exportPeriodReport(org.getId(), from, to);
            return buildExcelResponse(bytes, "period_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/returns")
    public ResponseEntity<byte[]> exportReturnsReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            byte[] bytes = excelExportService.exportReturnsReport(org.getId(), from, to);
            return buildExcelResponse(bytes, "returns_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/delivery-time")
    public ResponseEntity<byte[]> exportDeliveryTimeReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            byte[] bytes = excelExportService.exportDeliveryTimeReport(org.getId(), from, to);
            return buildExcelResponse(bytes, "delivery_time_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trips")
    public ResponseEntity<byte[]> exportTripReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            byte[] bytes = excelExportService.exportTripReport(org.getId(), from, to);
            return buildExcelResponse(bytes, "trip_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/performance/couriers")
    public ResponseEntity<byte[]> exportCourierPerformance(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            PerformanceComparison report = reportingService.getCourierPerformanceComparison(org, businessDayId, from, to);
            byte[] bytes = excelExportService.exportPerformanceComparison(report, "مقارنة أداء المناديب");
            return buildExcelResponse(bytes, "courier_performance.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/performance/organizations")
    public ResponseEntity<byte[]> exportOrgPerformance(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            PerformanceComparison report = reportingService.getOrganizationPerformanceComparison(org, businessDayId, from, to);
            byte[] bytes = excelExportService.exportPerformanceComparison(report, "مقارنة أداء المنظمات");
            return buildExcelResponse(bytes, "org_performance.xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/financial")
    public ResponseEntity<byte[]> exportFinancialReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            com.shipment.shippinggo.dto.report.FinancialSummaryReport report;
            if (businessDayId != null) {
                report = reportingService.getFinancialSummaryByBusinessDay(org, businessDayId);
            } else {
                report = reportingService.getFinancialSummaryByPeriod(org, from, to);
            }
            byte[] bytes = excelExportService.exportFinancialReport(report);
            return buildExcelResponse(bytes, "financial_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/geographic")
    public ResponseEntity<byte[]> exportGeographicReport(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) return ResponseEntity.badRequest().build();

        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();

        try {
            com.shipment.shippinggo.dto.report.GeographicReport report =
                    reportingService.getGeographicReport(org.getId(), from, to);
            byte[] bytes = excelExportService.exportGeographicReport(report);
            return buildExcelResponse(bytes, "geographic_report_" + from + "_" + to + ".xlsx");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
