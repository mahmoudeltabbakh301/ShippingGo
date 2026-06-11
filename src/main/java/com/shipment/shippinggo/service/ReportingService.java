package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.dto.report.*;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrganizationService organizationService;
    private final AccountSummaryService accountSummaryService;
    private final BusinessDayService businessDayService;
    private final CommissionService commissionService;

    // =====================================================================
    // 1. تقرير الفترة الزمنية (Period Report) - مدعوم بيوم عمل وفترة
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'period_' + #orgId + '_' + #from + '_' + #to")
    public PeriodReport getPeriodReport(Long orgId, LocalDate from, LocalDate to) {
        com.shipment.shippinggo.dto.PeriodStatsQueryResult stats =
                orderRepository.getPeriodStatsByOwnerOrg(orgId, from, to);
        return buildPeriodReportFromStats(stats, from, to);
    }

    @Cacheable(value = "reportsCache", key = "'period_bd_' + #orgId + '_' + #businessDayId")
    public PeriodReport getPeriodReportByBusinessDay(Long orgId, Long businessDayId) {
        List<Order> orders = orderService.getOrdersByBusinessDay(businessDayId);
        // فلترة الأوردرات المملوكة + المسندة لهذه المنظمة
        orders = orders.stream()
                .filter(o -> o.getOwnerOrganization().getId().equals(orgId)
                        || (o.getAssignedToOrganization() != null
                        && o.getAssignedToOrganization().getId().equals(orgId)))
                .collect(Collectors.toList());

        BusinessDay bd = businessDayService.getById(businessDayId);
        LocalDate date = bd != null ? bd.getDate() : LocalDate.now();
        return buildPeriodReportFromOrders(orders, date, date);
    }

    // =====================================================================
    // 2. التقرير المالي الشامل (Financial Summary) - الصافي الكلي
    // =====================================================================

    /**
     * تقرير مالي شامل بناءً على يوم عمل
     * يستخدم AccountSummaryService لضمان تطابق الأرقام مع صفحة الحسابات
     */
    @Cacheable(value = "reportsCache", key = "'financial_bd_' + #org.id + '_' + #businessDayId")
    public FinancialSummaryReport getFinancialSummaryByBusinessDay(Organization org, Long businessDayId) {
        List<Organization> linkedOrgs = organizationService.getLinkedOrganizations(org);
        List<User> couriers = organizationService.getCouriersByOrganization(org);

        List<AccountSummaryDTO> accountSummaries =
                accountSummaryService.getAllAccountSummariesByBusinessDay(org, linkedOrgs, couriers, businessDayId);

        BusinessDay bd = businessDayService.getById(businessDayId);

        return buildFinancialSummary(org, accountSummaries, bd, null, null);
    }

    /**
     * تقرير مالي شامل بناءً على فترة زمنية
     * يجمع حسابات أيام العمل في الفترة المحددة
     */
    @Cacheable(value = "reportsCache", key = "'financial_period_' + #org.id + '_' + #from + '_' + #to")
    public FinancialSummaryReport getFinancialSummaryByPeriod(Organization org, LocalDate from, LocalDate to) {
        List<Organization> linkedOrgs = organizationService.getLinkedOrganizations(org);
        List<User> couriers = organizationService.getCouriersByOrganization(org);

        // جلب أيام العمل في الفترة
        List<BusinessDay> businessDays = businessDayService.getBusinessDays(org.getId()).stream()
                .filter(bd -> !bd.getDate().isBefore(from) && !bd.getDate().isAfter(to))
                .collect(Collectors.toList());

        // تجميع كل الملخصات من كل يوم عمل
        List<AccountSummaryDTO> allSummaries = new ArrayList<>();
        for (BusinessDay bd : businessDays) {
            allSummaries.addAll(
                    accountSummaryService.getAllAccountSummariesByBusinessDay(
                            org, linkedOrgs, couriers, bd.getId()));
        }

        return buildFinancialSummary(org, allSummaries, null, from, to);
    }

    // =====================================================================
    // 3. تقرير المندوب (Courier Report) - تفصيلي
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'courier_bd_' + #org.id + '_' + #courierId + '_' + #businessDayId")
    public CourierReport getCourierReportByBusinessDay(Organization org, Long courierId, Long businessDayId) {
        User courier = organizationService.getUserById(courierId);
        if (courier == null) return CourierReport.builder().build();

        AccountSummaryDTO summary =
                accountSummaryService.getCourierAccountSummaryByBusinessDay(businessDayId, courier, org, null);

        BusinessDay bd = businessDayService.getById(businessDayId);
        List<Order> orders = bd != null
                ? accountSummaryService.getOrdersAssignedToCourierByBusinessDay(bd, courier)
                : Collections.emptyList();

        return buildCourierReport(courier, summary, orders, bd);
    }

    @Cacheable(value = "reportsCache", key = "'courier_period_' + #org.id + '_' + #courierId + '_' + #from + '_' + #to")
    public CourierReport getCourierReport(Organization org, Long courierId, LocalDate from, LocalDate to) {
        User courier = organizationService.getUserById(courierId);
        if (courier == null) return CourierReport.builder().build();

        AccountSummaryDTO summary = accountSummaryService.getCourierAccountSummary(courier, org);

        // فلترة حسب التاريخ
        List<Order> allOrders = accountSummaryService.getOrdersAssignedToCourier(courier);
        List<Order> orders = allOrders.stream()
                .filter(o -> {
                    LocalDate d = o.getOriginalCreationDate();
                    return d != null && !d.isBefore(from) && !d.isAfter(to);
                })
                .collect(Collectors.toList());

        return buildCourierReport(courier, summary, orders, null);
    }

    // =====================================================================
    // 4. تقرير المنظمة (Organization Report) - تفصيلي
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'org_bd_' + #org.id + '_' + #targetOrgId + '_' + #direction + '_' + #businessDayId")
    public OrganizationReport getOrganizationReportByBusinessDay(Organization org, Long targetOrgId,
                                                                  String direction, Long businessDayId) {
        Organization targetOrg = organizationService.getById(targetOrgId);
        if (targetOrg == null) return OrganizationReport.builder().build();

        AccountSummaryDTO summary = accountSummaryService.getOrganizationAccountSummaryByBusinessDay(
                businessDayId, org,
                "INCOMING".equals(direction) ? targetOrg : null,
                "OUTGOING".equals(direction) ? targetOrg : null,
                direction);

        return buildOrganizationReport(targetOrg, summary, direction);
    }

    @Cacheable(value = "reportsCache", key = "'org_period_' + #org.id + '_' + #targetOrgId + '_' + #direction + '_' + #from + '_' + #to")
    public OrganizationReport getOrganizationReport(Organization org, Long targetOrgId,
                                                     String direction, LocalDate from, LocalDate to) {
        Organization targetOrg = organizationService.getById(targetOrgId);
        if (targetOrg == null) return OrganizationReport.builder().build();

        AccountSummaryDTO summary = accountSummaryService.getOrganizationAccountSummary(
                org,
                "INCOMING".equals(direction) ? targetOrg : null,
                "OUTGOING".equals(direction) ? targetOrg : null,
                direction);

        return buildOrganizationReport(targetOrg, summary, direction);
    }

    // =====================================================================
    // 5. مقارنة أداء المناديب
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'perf_couriers_' + #org.id + '_' + #businessDayId")
    public PerformanceComparison getCourierPerformanceComparison(Organization org, Long businessDayId) {
        List<User> couriers = organizationService.getCouriersByOrganization(org);

        List<PerformanceComparison.PerformanceEntry> entries = new ArrayList<>();
        for (User courier : couriers) {
            AccountSummaryDTO summary;
            if (businessDayId != null) {
                summary = accountSummaryService.getCourierAccountSummaryByBusinessDay(
                        businessDayId, courier, org, null);
            } else {
                summary = accountSummaryService.getCourierAccountSummary(courier, org);
            }

            if (summary.getTotalOrders() > 0) {
                entries.add(buildPerformanceEntry(courier.getId(), courier.getFullName(),
                        "courier", summary));
            }
        }

        return buildPerformanceComparison("couriers", entries);
    }

    // =====================================================================
    // 6. مقارنة أداء المنظمات
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'perf_orgs_' + #org.id + '_' + #businessDayId")
    public PerformanceComparison getOrganizationPerformanceComparison(Organization org, Long businessDayId) {
        List<Organization> linkedOrgs = organizationService.getLinkedOrganizations(org);

        List<PerformanceComparison.PerformanceEntry> entries = new ArrayList<>();
        for (Organization linkedOrg : linkedOrgs) {
            AccountSummaryDTO summary;
            if (businessDayId != null) {
                summary = accountSummaryService.getOrganizationAccountSummaryByBusinessDay(
                        businessDayId, org, null, linkedOrg, "OUTGOING");
            } else {
                summary = accountSummaryService.getOrganizationAccountSummary(
                        org, null, linkedOrg, "OUTGOING");
            }

            if (summary.getTotalOrders() > 0) {
                entries.add(buildPerformanceEntry(linkedOrg.getId(), linkedOrg.getName(),
                        "organization", summary));
            }
        }

        return buildPerformanceComparison("organizations", entries);
    }

    // =====================================================================
    // 7. التقرير الجغرافي (محسّن)
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'geo_period_' + #orgId + '_' + #from + '_' + #to")
    public GeographicReport getGeographicReport(Long orgId, LocalDate from, LocalDate to) {
        List<com.shipment.shippinggo.dto.GeographicStatsQueryResult> queryResults =
                orderRepository.getGeographicStatsByOwnerOrg(orgId, from, to);

        Map<Governorate, GeographicReport.GovernorateStats> stats = new HashMap<>();
        for (com.shipment.shippinggo.dto.GeographicStatsQueryResult result : queryResults) {
            long total = result.getTotalOrders() != null ? result.getTotalOrders() : 0;
            long delivered = result.getDeliveredOrders() != null ? result.getDeliveredOrders() : 0;
            BigDecimal revenue = result.getTotalRevenue() != null ? result.getTotalRevenue() : BigDecimal.ZERO;
            double rate = total > 0 ? (delivered * 100.0 / total) : 0;

            stats.put(result.getGovernorate(), GeographicReport.GovernorateStats.builder()
                    .totalOrders(total)
                    .deliveryRate(Math.round(rate * 100.0) / 100.0)
                    .totalRevenue(revenue)
                    .build());
        }

        return GeographicReport.builder().statsByGovernorate(stats).build();
    }

    @Cacheable(value = "reportsCache", key = "'geo_bd_' + #orgId + '_' + #businessDayId")
    public GeographicReport getGeographicReportByBusinessDay(Long orgId, Long businessDayId) {
        List<Order> orders = orderService.getOrdersByBusinessDay(businessDayId).stream()
                .filter(o -> o.getOwnerOrganization().getId().equals(orgId))
                .collect(Collectors.toList());

        Map<Governorate, GeographicReport.GovernorateStats> stats = new HashMap<>();
        Map<Governorate, long[]> counters = new HashMap<>(); // [total, delivered]
        Map<Governorate, BigDecimal> revenues = new HashMap<>();

        for (Order order : orders) {
            Governorate gov = order.getGovernorate();
            if (gov == null) continue;

            counters.computeIfAbsent(gov, k -> new long[]{0, 0});
            revenues.computeIfAbsent(gov, k -> BigDecimal.ZERO);

            counters.get(gov)[0]++;
            if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                counters.get(gov)[1]++;
                BigDecimal amt = order.getStatus() == OrderStatus.PARTIAL_DELIVERY
                        ? (order.getPartialDeliveryAmount() != null ? order.getPartialDeliveryAmount() : BigDecimal.ZERO)
                        : (order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                revenues.put(gov, revenues.get(gov).add(amt));
            }
        }

        for (Map.Entry<Governorate, long[]> entry : counters.entrySet()) {
            long total = entry.getValue()[0];
            long delivered = entry.getValue()[1];
            double rate = total > 0 ? (delivered * 100.0 / total) : 0;

            stats.put(entry.getKey(), GeographicReport.GovernorateStats.builder()
                    .totalOrders(total)
                    .deliveryRate(Math.round(rate * 100.0) / 100.0)
                    .totalRevenue(revenues.getOrDefault(entry.getKey(), BigDecimal.ZERO))
                    .build());
        }

        return GeographicReport.builder().statsByGovernorate(stats).build();
    }

    // =====================================================================
    // 8. تقرير الاتجاهات (Trends)
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'trends_' + #orgId + '_' + #from + '_' + #to")
    public TrendData getTrendsReport(Long orgId, LocalDate from, LocalDate to) {
        List<com.shipment.shippinggo.dto.TrendStatsQueryResult> queryResults =
                orderRepository.getTrendStatsByOwnerOrg(orgId, from, to);

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (com.shipment.shippinggo.dto.TrendStatsQueryResult result : queryResults) {
            LocalDate date = result.getCreationDate();
            if (date != null) {
                labels.add(date.format(fmt));
                counts.add(result.getTotalOrders() != null ? result.getTotalOrders() : 0);
                revenues.add(result.getTotalRevenue() != null ? result.getTotalRevenue() : BigDecimal.ZERO);
            }
        }

        return TrendData.builder()
                .labels(labels)
                .orderCounts(counts)
                .revenues(revenues)
                .build();
    }

    // =====================================================================
    // === Private Builder Methods ===
    // =====================================================================

    private PeriodReport buildPeriodReportFromStats(
            com.shipment.shippinggo.dto.PeriodStatsQueryResult stats,
            LocalDate from, LocalDate to) {

        long total = stats.getTotalCount() != null ? stats.getTotalCount() : 0;
        long delivered = stats.getDeliveredCount() != null ? stats.getDeliveredCount() : 0;
        long refused = stats.getRefusedCount() != null ? stats.getRefusedCount() : 0;
        long cancelled = stats.getCancelledCount() != null ? stats.getCancelledCount() : 0;
        long deferred = stats.getDeferredCount() != null ? stats.getDeferredCount() : 0;
        long partial = stats.getPartialCount() != null ? stats.getPartialCount() : 0;
        long inTransit = stats.getInTransitCount() != null ? stats.getInTransitCount() : 0;
        BigDecimal collected = stats.getTotalCollected() != null ? stats.getTotalCollected() : BigDecimal.ZERO;

        double successRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
        double refRate = total > 0 ? (refused * 100.0 / total) : 0;
        BigDecimal avgValue = total > 0
                ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PeriodReport.builder()
                .fromDate(from).toDate(to)
                .totalOrders(total).delivered(delivered).refused(refused)
                .cancelled(cancelled).deferred(deferred).partial(partial).inTransit(inTransit)
                .totalCollected(collected)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();
    }

    private PeriodReport buildPeriodReportFromOrders(List<Order> orders, LocalDate from, LocalDate to) {
        long total = orders.size();
        long delivered = 0, refused = 0, cancelled = 0, deferred = 0, partial = 0, inTransit = 0;
        BigDecimal collected = BigDecimal.ZERO;

        for (Order o : orders) {
            switch (o.getStatus()) {
                case DELIVERED -> {
                    delivered++;
                    collected = collected.add(o.getCollectedAmount() != null ? o.getCollectedAmount()
                            : (o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO));
                }
                case PARTIAL_DELIVERY -> {
                    partial++;
                    collected = collected.add(
                            o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount() : BigDecimal.ZERO);
                }
                case REFUSED -> refused++;
                case CANCELLED -> cancelled++;
                case DEFERRED -> deferred++;
                case IN_TRANSIT -> inTransit++;
                default -> {}
            }
        }

        double successRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
        double refRate = total > 0 ? (refused * 100.0 / total) : 0;
        BigDecimal avgValue = total > 0
                ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PeriodReport.builder()
                .fromDate(from).toDate(to)
                .totalOrders(total).delivered(delivered).refused(refused)
                .cancelled(cancelled).deferred(deferred).partial(partial).inTransit(inTransit)
                .totalCollected(collected)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();
    }

    private FinancialSummaryReport buildFinancialSummary(Organization org,
                                                          List<AccountSummaryDTO> accountSummaries,
                                                          BusinessDay bd,
                                                          LocalDate from, LocalDate to) {
        // تصنيف الملخصات
        List<AccountSummaryDTO> outgoingOrgs = accountSummaries.stream()
                .filter(s -> "OUTGOING".equals(s.getDirection())).collect(Collectors.toList());
        List<AccountSummaryDTO> incomingOrgs = accountSummaries.stream()
                .filter(s -> "INCOMING".equals(s.getDirection())).collect(Collectors.toList());
        List<AccountSummaryDTO> courierSummaries = accountSummaries.stream()
                .filter(s -> "courier".equals(s.getType())).collect(Collectors.toList());
        List<AccountSummaryDTO> unassignedSummaries = accountSummaries.stream()
                .filter(s -> "UNASSIGNED".equals(s.getDirection())).collect(Collectors.toList());

        // === عمولات صادرة (منظمات) - مفصّلة ===
        BigDecimal outDeliveryComm = outgoingOrgs.stream()
                .map(s -> s.getDeliveryCommission() != null ? s.getDeliveryCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outRejectionComm = outgoingOrgs.stream()
                .map(s -> s.getRejectionCommission() != null ? s.getRejectionCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outCancellationComm = outgoingOrgs.stream()
                .map(s -> s.getCancellationCommission() != null ? s.getCancellationCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outOrgComm = outDeliveryComm.add(outRejectionComm).add(outCancellationComm);

        // === عمولات واردة (منظمات) - مفصّلة ===
        BigDecimal inDeliveryComm = incomingOrgs.stream()
                .map(s -> s.getDeliveryCommission() != null ? s.getDeliveryCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inRejectionComm = incomingOrgs.stream()
                .map(s -> s.getRejectionCommission() != null ? s.getRejectionCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inCancellationComm = incomingOrgs.stream()
                .map(s -> s.getCancellationCommission() != null ? s.getCancellationCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal inOrgComm = inDeliveryComm.add(inRejectionComm).add(inCancellationComm);

        // === عمولات مناديب - مفصّلة ===
        BigDecimal cDeliveryComm = courierSummaries.stream()
                .map(s -> s.getDeliveryCommission() != null ? s.getDeliveryCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cRejectionComm = courierSummaries.stream()
                .map(s -> s.getRejectionCommission() != null ? s.getRejectionCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cCancellationComm = courierSummaries.stream()
                .map(s -> s.getCancellationCommission() != null ? s.getCancellationCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal courierComm = cDeliveryComm.add(cRejectionComm).add(cCancellationComm);

        // === عمولات غير مسندة - مفصّلة ===
        BigDecimal uDeliveryComm = unassignedSummaries.stream()
                .map(s -> s.getDeliveryCommission() != null ? s.getDeliveryCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal uRejectionComm = unassignedSummaries.stream()
                .map(s -> s.getRejectionCommission() != null ? s.getRejectionCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal uCancellationComm = unassignedSummaries.stream()
                .map(s -> s.getCancellationCommission() != null ? s.getCancellationCommission() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unassignedComm = uDeliveryComm.add(uRejectionComm).add(uCancellationComm);

        // === حساب صافي ربح المنظمة ===
        // إجمالي إيرادات العمولات = عمولات الصادر + عمولات الوارد + عمولات غير مسندة
        BigDecimal totalCommissionRevenue = outOrgComm.add(inOrgComm).add(unassignedComm);
        // صافي الربح = إجمالي إيرادات العمولات − عمولات المناديب
        BigDecimal netProfit = totalCommissionRevenue.subtract(courierComm);

        // إحصائيات عامة من كل الأوردرات
        long totalOrders = accountSummaries.stream().mapToLong(AccountSummaryDTO::getTotalOrders).sum();
        long deliveredOrders = accountSummaries.stream().mapToLong(AccountSummaryDTO::getDeliveredOrders).sum();
        long refusedOrders = accountSummaries.stream().mapToLong(s -> s.getRefusedOrders()).sum();
        long cancelledOrders = accountSummaries.stream().mapToLong(AccountSummaryDTO::getCancelledOrders).sum();

        // تفاصيل لكل منظمة/مندوب
        List<FinancialSummaryReport.EntityFinancialDetail> outDetails = outgoingOrgs.stream()
                .map(this::toEntityDetail).collect(Collectors.toList());
        List<FinancialSummaryReport.EntityFinancialDetail> inDetails = incomingOrgs.stream()
                .map(this::toEntityDetail).collect(Collectors.toList());
        List<FinancialSummaryReport.EntityFinancialDetail> courierDetails = courierSummaries.stream()
                .map(this::toEntityDetail).collect(Collectors.toList());

        return FinancialSummaryReport.builder()
                .businessDayId(bd != null ? bd.getId() : null)
                .businessDayName(bd != null ? bd.getName() : null)
                .fromDate(from != null ? from : (bd != null ? bd.getDate() : null))
                .toDate(to != null ? to : (bd != null ? bd.getDate() : null))
                .totalOrders(totalOrders)
                .deliveredOrders(deliveredOrders)
                .refusedOrders(refusedOrders)
                .cancelledOrders(cancelledOrders)
                // عمولات الصادر
                .outgoingOrgCommissions(outOrgComm)
                .outgoingDeliveryComm(outDeliveryComm)
                .outgoingRejectionComm(outRejectionComm)
                .outgoingCancellationComm(outCancellationComm)
                // عمولات الوارد
                .incomingOrgCommissions(inOrgComm)
                .incomingDeliveryComm(inDeliveryComm)
                .incomingRejectionComm(inRejectionComm)
                .incomingCancellationComm(inCancellationComm)
                // عمولات المناديب
                .courierCommissions(courierComm)
                .courierDeliveryComm(cDeliveryComm)
                .courierRejectionComm(cRejectionComm)
                .courierCancellationComm(cCancellationComm)
                // عمولات غير مسندة
                .unassignedCommissions(unassignedComm)
                .unassignedDeliveryComm(uDeliveryComm)
                .unassignedRejectionComm(uRejectionComm)
                .unassignedCancellationComm(uCancellationComm)
                // الإجماليات والصافي
                .totalCommissionRevenue(totalCommissionRevenue)
                .totalOutgoingCommissions(outOrgComm)
                .totalIncomingCommissions(inOrgComm)
                .netProfit(netProfit)
                .outgoingOrgDetails(outDetails)
                .incomingOrgDetails(inDetails)
                .courierDetails(courierDetails)
                .build();
    }


    private FinancialSummaryReport.EntityFinancialDetail toEntityDetail(AccountSummaryDTO s) {
        return FinancialSummaryReport.EntityFinancialDetail.builder()
                .entityId(s.getId())
                .entityName(s.getName())
                .entityType(s.getType())
                .totalOrders(s.getTotalOrders())
                .deliveredOrders(s.getDeliveredOrders())
                .refusedOrders(s.getRefusedOrders())
                .cancelledOrders(s.getCancelledOrders())
                .deliveredAmount(s.getDeliveredAmount() != null ? s.getDeliveredAmount() : BigDecimal.ZERO)
                .deliveryCommission(s.getDeliveryCommission() != null ? s.getDeliveryCommission() : BigDecimal.ZERO)
                .rejectionCommission(s.getRejectionCommission() != null ? s.getRejectionCommission() : BigDecimal.ZERO)
                .cancellationCommission(s.getCancellationCommission() != null ? s.getCancellationCommission() : BigDecimal.ZERO)
                .totalCommission(s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .netAmount(s.getNetAmount() != null ? s.getNetAmount() : BigDecimal.ZERO)
                .build();
    }

    private CourierReport buildCourierReport(User courier, AccountSummaryDTO summary,
                                              List<Order> orders, BusinessDay bd) {
        BigDecimal deliveryComm = summary.getDeliveryCommission() != null
                ? summary.getDeliveryCommission() : BigDecimal.ZERO;
        BigDecimal rejectionComm = summary.getRejectionCommission() != null
                ? summary.getRejectionCommission() : BigDecimal.ZERO;
        BigDecimal cancellationComm = summary.getCancellationCommission() != null
                ? summary.getCancellationCommission() : BigDecimal.ZERO;
        BigDecimal totalComm = deliveryComm.add(rejectionComm).add(cancellationComm);

        // المبالغ المحصّلة حسب النوع
        BigDecimal collectedDelivered = BigDecimal.ZERO;
        BigDecimal collectedPartial = BigDecimal.ZERO;
        BigDecimal collectedRejection = BigDecimal.ZERO;

        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.DELIVERED) {
                collectedDelivered = collectedDelivered.add(
                        o.getCollectedAmount() != null ? o.getCollectedAmount()
                                : (o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO));
            } else if (o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                collectedPartial = collectedPartial.add(
                        o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount() : BigDecimal.ZERO);
            } else if (o.getStatus() == OrderStatus.REFUSED) {
                collectedRejection = collectedRejection.add(
                        o.getRejectionPayment() != null ? o.getRejectionPayment() : BigDecimal.ZERO);
            }
        }

        BigDecimal totalCollected = collectedDelivered.add(collectedPartial).add(collectedRejection);
        BigDecimal amountDue = totalCollected.subtract(totalComm);

        // جغرافيا
        Map<Governorate, Long> byGov = orders.stream()
                .filter(o -> o.getGovernorate() != null)
                .collect(Collectors.groupingBy(Order::getGovernorate, Collectors.counting()));

        long total = summary.getTotalOrders();
        long delivered = summary.getDeliveredOrders();
        long refused = summary.getRefusedOrders();

        double deliveryRate = total > 0 ? ((delivered) * 100.0 / total) : 0;
        double refusalRate = total > 0 ? (refused * 100.0 / total) : 0;

        LocalDate date = bd != null ? bd.getDate() : LocalDate.now();
        PeriodReport period = buildPeriodReportFromOrders(orders, date, date);

        return CourierReport.builder()
                .courierId(courier.getId())
                .courierName(courier.getFullName())
                .periodStats(period)
                .deliveryCommission(deliveryComm)
                .rejectionCommission(rejectionComm)
                .cancellationCommission(cancellationComm)
                .totalCommission(totalComm)
                .collectedDelivered(collectedDelivered)
                .collectedPartial(collectedPartial)
                .collectedRejection(collectedRejection)
                .totalCollected(totalCollected)
                .amountDue(amountDue)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .refusalRate(Math.round(refusalRate * 100.0) / 100.0)
                .ordersByGovernorate(byGov)
                .build();
    }

    private OrganizationReport buildOrganizationReport(Organization targetOrg,
                                                        AccountSummaryDTO summary, String direction) {
        long total = summary.getTotalOrders();
        long delivered = summary.getDeliveredOrders();
        long refused = summary.getRefusedOrders();

        double deliveryRate = total > 0 ? (delivered * 100.0 / total) : 0;
        double refusalRate = total > 0 ? (refused * 100.0 / total) : 0;

        return OrganizationReport.builder()
                .organizationId(targetOrg.getId())
                .organizationName(targetOrg.getName())
                .direction(direction)
                .totalOrders(total)
                .deliveredOrders(delivered)
                .refusedOrders(refused)
                .cancelledOrders(summary.getCancelledOrders())
                .totalOrderAmount(summary.getTotalAmount() != null ? summary.getTotalAmount() : BigDecimal.ZERO)
                .deliveredAmount(summary.getDeliveredAmount() != null ? summary.getDeliveredAmount() : BigDecimal.ZERO)
                .rejectionPayments(summary.getReturnedAmount() != null ? summary.getReturnedAmount() : BigDecimal.ZERO)
                .deliveryCommission(summary.getDeliveryCommission() != null ? summary.getDeliveryCommission() : BigDecimal.ZERO)
                .rejectionCommission(summary.getRejectionCommission() != null ? summary.getRejectionCommission() : BigDecimal.ZERO)
                .cancellationCommission(summary.getCancellationCommission() != null ? summary.getCancellationCommission() : BigDecimal.ZERO)
                .totalCommission(summary.getTotalCommissions() != null ? summary.getTotalCommissions() : BigDecimal.ZERO)
                .netAmount(summary.getNetAmount() != null ? summary.getNetAmount() : BigDecimal.ZERO)
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .refusalRate(Math.round(refusalRate * 100.0) / 100.0)
                .build();
    }

    private PerformanceComparison.PerformanceEntry buildPerformanceEntry(
            Long id, String name, String type, AccountSummaryDTO summary) {

        long total = summary.getTotalOrders();
        long delivered = summary.getDeliveredOrders();
        long refused = summary.getRefusedOrders();

        double deliveryRate = total > 0 ? (delivered * 100.0 / total) : 0;
        double refusalRate = total > 0 ? (refused * 100.0 / total) : 0;

        return PerformanceComparison.PerformanceEntry.builder()
                .entityId(id)
                .entityName(name)
                .entityType(type)
                .totalOrders(total)
                .deliveredOrders(delivered)
                .refusedOrders(refused)
                .cancelledOrders(summary.getCancelledOrders())
                .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                .refusalRate(Math.round(refusalRate * 100.0) / 100.0)
                .totalCollected(summary.getDeliveredAmount() != null ? summary.getDeliveredAmount() : BigDecimal.ZERO)
                .totalCommission(summary.getTotalCommissions() != null ? summary.getTotalCommissions() : BigDecimal.ZERO)
                .netAmount(summary.getNetAmount() != null ? summary.getNetAmount() : BigDecimal.ZERO)
                .build();
    }

    private PerformanceComparison buildPerformanceComparison(String type,
                                                              List<PerformanceComparison.PerformanceEntry> entries) {
        // ترتيب حسب معدل التوصيل (تنازلي)
        entries.sort((a, b) -> Double.compare(b.getDeliveryRate(), a.getDeliveryRate()));

        // ترقيم
        for (int i = 0; i < entries.size(); i++) {
            entries.get(i).setRank(i + 1);
        }

        PerformanceComparison.PerformanceEntry top = entries.isEmpty() ? null : entries.get(0);
        PerformanceComparison.PerformanceEntry worst = entries.isEmpty() ? null : entries.get(entries.size() - 1);

        double avgRate = entries.stream().mapToDouble(PerformanceComparison.PerformanceEntry::getDeliveryRate)
                .average().orElse(0);
        double avgCount = entries.stream().mapToLong(PerformanceComparison.PerformanceEntry::getTotalOrders)
                .average().orElse(0);

        return PerformanceComparison.builder()
                .comparisonType(type)
                .entries(entries)
                .topPerformer(top)
                .worstPerformer(worst)
                .averageDeliveryRate(Math.round(avgRate * 100.0) / 100.0)
                .averageOrderCount(Math.round(avgCount * 100.0) / 100.0)
                .build();
    }
}