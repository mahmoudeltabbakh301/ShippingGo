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
import com.shipment.shippinggo.repository.OrderReportRepository;
import com.shipment.shippinggo.repository.TripRepository;
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
    private final OrderReportRepository orderReportRepository;
    private final OrganizationService organizationService;
    private final AccountSummaryService accountSummaryService;
    private final BusinessDayService businessDayService;
    private final CommissionService commissionService;
    private final TripRepository tripRepository;

    // =====================================================================
    // 1. تقرير الفترة الزمنية (Period Report) - مدعوم بيوم عمل وفترة
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'period_' + #orgId + '_' + #from + '_' + #to")
    public PeriodReport getPeriodReport(Long orgId, LocalDate from, LocalDate to) {
        com.shipment.shippinggo.dto.PeriodStatsQueryResult stats =
                orderReportRepository.getPeriodStatsByOwnerOrg(orgId, from, to);
        return buildPeriodReportFromStats(orgId, stats, from, to);
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

    @Cacheable(value = "reportsCache", key = "'perf_couriers_' + #org.id + '_' + #businessDayId + '_' + #from + '_' + #to")
    public PerformanceComparison getCourierPerformanceComparison(Organization org, Long businessDayId, LocalDate from, LocalDate to) {
        List<User> couriers = organizationService.getCouriersByOrganization(org);
        List<PerformanceComparison.PerformanceEntry> entries = new ArrayList<>();

        for (User courier : couriers) {
            if (businessDayId != null) {
                AccountSummaryDTO summary = accountSummaryService.getCourierAccountSummaryByBusinessDay(businessDayId, courier, org, null);
                if (summary.getTotalOrders() > 0) {
                    entries.add(buildPerformanceEntry(courier.getId(), courier.getFullName(), "courier", summary));
                }
            } else {
                com.shipment.shippinggo.dto.PeriodStatsQueryResult stats = orderReportRepository.getPeriodStatsByCourierId(courier.getId(), from, to);
                long total = stats.getTotalCount() != null ? stats.getTotalCount() : 0;
                
                if (total > 0) {
                    BigDecimal commissions = orderReportRepository.getTotalCommissionByCourierId(courier.getId(), from, to);
                    if (commissions == null) commissions = BigDecimal.ZERO;

                    long delivered = stats.getDeliveredCount() != null ? stats.getDeliveredCount() : 0;
                    long refused = stats.getRefusedCount() != null ? stats.getRefusedCount() : 0;
                    long cancelled = stats.getCancelledCount() != null ? stats.getCancelledCount() : 0;
                    long deferred = stats.getDeferredCount() != null ? stats.getDeferredCount() : 0;
                    long partial = stats.getPartialCount() != null ? stats.getPartialCount() : 0;
                    
                    double deliveryRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
                    double refusalRate = total > 0 ? (refused * 100.0 / total) : 0;
                    BigDecimal totalCollected = stats.getTotalCollected() != null ? stats.getTotalCollected() : BigDecimal.ZERO;

                    entries.add(PerformanceComparison.PerformanceEntry.builder()
                            .entityId(courier.getId())
                            .entityName(courier.getFullName())
                            .entityType("courier")
                            .totalOrders(total)
                            .deliveredOrders(delivered)
                            .refusedOrders(refused)
                            .cancelledOrders(cancelled)
                            .deferredOrders(deferred)
                            .partialDeliveryOrders(partial)
                            .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                            .refusalRate(Math.round(refusalRate * 100.0) / 100.0)
                            .totalCollected(totalCollected)
                            .totalCommission(commissions)
                            .netAmount(totalCollected.subtract(commissions))
                            .build());
                }
            }
        }

        return buildPerformanceComparison("couriers", entries);
    }

    // =====================================================================
    // 6. مقارنة أداء المنظمات
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'perf_orgs_' + #org.id + '_' + #businessDayId + '_' + #from + '_' + #to")
    public PerformanceComparison getOrganizationPerformanceComparison(Organization org, Long businessDayId, LocalDate from, LocalDate to) {
        List<Organization> linkedOrgs = organizationService.getLinkedOrganizations(org);
        List<PerformanceComparison.PerformanceEntry> entries = new ArrayList<>();

        for (Organization linkedOrg : linkedOrgs) {
            if (businessDayId != null) {
                AccountSummaryDTO summary = accountSummaryService.getOrganizationAccountSummaryByBusinessDay(
                        businessDayId, org, null, linkedOrg, "OUTGOING");
                if (summary.getTotalOrders() > 0) {
                    String orgName = (linkedOrg.getName() != null && !linkedOrg.getName().isEmpty()) ? linkedOrg.getName() : "منظمة " + linkedOrg.getId();
                    entries.add(buildPerformanceEntry(linkedOrg.getId(), orgName, "organization", summary));
                }
            } else {
                com.shipment.shippinggo.dto.PeriodStatsQueryResult stats = orderReportRepository.getPeriodStatsByOwnerAndAssignedOrg(org.getId(), linkedOrg.getId(), from, to);
                long total = stats.getTotalCount() != null ? stats.getTotalCount() : 0;
                
                if (total > 0) {
                    BigDecimal commissions = orderReportRepository.getTotalCommissionByOwnerAndAssignedOrg(org.getId(), linkedOrg.getId(), from, to);
                    if (commissions == null) commissions = BigDecimal.ZERO;

                    long delivered = stats.getDeliveredCount() != null ? stats.getDeliveredCount() : 0;
                    long refused = stats.getRefusedCount() != null ? stats.getRefusedCount() : 0;
                    long cancelled = stats.getCancelledCount() != null ? stats.getCancelledCount() : 0;
                    long deferred = stats.getDeferredCount() != null ? stats.getDeferredCount() : 0;
                    long partial = stats.getPartialCount() != null ? stats.getPartialCount() : 0;
                    
                    double deliveryRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
                    double refusalRate = total > 0 ? (refused * 100.0 / total) : 0;
                    BigDecimal totalCollected = stats.getTotalCollected() != null ? stats.getTotalCollected() : BigDecimal.ZERO;

                    String orgName = (linkedOrg.getName() != null && !linkedOrg.getName().isEmpty()) ? linkedOrg.getName() : "منظمة " + linkedOrg.getId();
                    entries.add(PerformanceComparison.PerformanceEntry.builder()
                            .entityId(linkedOrg.getId())
                            .entityName(orgName)
                            .entityType("organization")
                            .totalOrders(total)
                            .deliveredOrders(delivered)
                            .refusedOrders(refused)
                            .cancelledOrders(cancelled)
                            .deferredOrders(deferred)
                            .partialDeliveryOrders(partial)
                            .deliveryRate(Math.round(deliveryRate * 100.0) / 100.0)
                            .refusalRate(Math.round(refusalRate * 100.0) / 100.0)
                            .totalCollected(totalCollected)
                            .totalCommission(commissions)
                            .netAmount(totalCollected.subtract(commissions))
                            .build());
                }
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
                orderReportRepository.getGeographicStatsByOwnerOrg(orgId, from, to);

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
                BigDecimal amt;
                if (order.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                    amt = order.getPartialDeliveryAmount() != null ? order.getPartialDeliveryAmount() : BigDecimal.ZERO;
                } else {
                    // استخدام المبلغ المحصّل إن وُجد، وإلا المبلغ الأصلي
                    amt = order.getCollectedAmount() != null ? order.getCollectedAmount()
                            : (order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                }
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
                orderReportRepository.getTrendStatsByOwnerOrg(orgId, from, to);

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<Double> deliveryRates = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (com.shipment.shippinggo.dto.TrendStatsQueryResult result : queryResults) {
            LocalDate date = result.getCreationDate();
            if (date != null) {
                long total = result.getTotalOrders() != null ? result.getTotalOrders() : 0;
                long delivered = result.getDeliveredOrders() != null ? result.getDeliveredOrders() : 0;
                double rate = total > 0 ? (delivered * 100.0 / total) : 0;

                labels.add(date.format(fmt));
                counts.add(total);
                revenues.add(result.getTotalRevenue() != null ? result.getTotalRevenue() : BigDecimal.ZERO);
                deliveryRates.add(Math.round(rate * 100.0) / 100.0);
            }
        }

        return TrendData.builder()
                .labels(labels)
                .orderCounts(counts)
                .revenues(revenues)
                .deliveryRates(deliveryRates)
                .build();
    }

    // =====================================================================
    // === Private Builder Methods ===
    // =====================================================================

    private PeriodReport buildPeriodReportFromStats(
            Long orgId,
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

        // حساب العمولات من الأوردرات مباشرة (sum of manualCourierCommission + manualOrgCommission)
        BigDecimal totalCommissions = orderReportRepository.getTotalCommissionsByOwnerOrg(orgId, from, to);
        if (totalCommissions == null) totalCommissions = BigDecimal.ZERO;
        BigDecimal netProfit = collected.subtract(totalCommissions);

        return PeriodReport.builder()
                .fromDate(from).toDate(to)
                .totalOrders(total).delivered(delivered).refused(refused)
                .cancelled(cancelled).deferred(deferred).partial(partial).inTransit(inTransit)
                .totalCollected(collected)
                .totalCommissions(totalCommissions)
                .netProfit(netProfit)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();
    }

    private PeriodReport buildPeriodReportFromOrders(List<Order> orders, LocalDate from, LocalDate to) {
        long total = orders.size();
        long delivered = 0, refused = 0, cancelled = 0, deferred = 0, partial = 0, inTransit = 0;
        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal totalCommissions = BigDecimal.ZERO;

        for (Order o : orders) {
            // حساب العمولات من الحقول المحفوظة في كل أوردر
            if (o.getManualCourierCommission() != null) {
                totalCommissions = totalCommissions.add(o.getManualCourierCommission());
            }
            if (o.getManualOrgCommission() != null) {
                totalCommissions = totalCommissions.add(o.getManualOrgCommission());
            }

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
                case REFUSED -> {
                    refused++;
                    collected = collected.add(
                            o.getRejectionPayment() != null ? o.getRejectionPayment() : BigDecimal.ZERO);
                }
                case CANCELLED -> cancelled++;
                case DEFERRED -> deferred++;
                case IN_TRANSIT, PICKED_UP, OUT_FOR_DELIVERY -> inTransit++;
                case RETURNED_TO_SENDER -> refused++;
                case WAITING -> {} // يُحسب ضمن الإجمالي فقط
                default -> {}
            }
        }

        double successRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
        double refRate = total > 0 ? (refused * 100.0 / total) : 0;
        BigDecimal avgValue = total > 0
                ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal netProfit = collected.subtract(totalCommissions);

        return PeriodReport.builder()
                .fromDate(from).toDate(to)
                .totalOrders(total).delivered(delivered).refused(refused)
                .cancelled(cancelled).deferred(deferred).partial(partial).inTransit(inTransit)
                .totalCollected(collected)
                .totalCommissions(totalCommissions)
                .netProfit(netProfit)
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

        // إحصائيات عامة دقيقة باستخدام PeriodStatsQueryResult
        com.shipment.shippinggo.dto.PeriodStatsQueryResult periodStats =
                orderReportRepository.getPeriodStatsByOwnerOrg(org.getId(),
                        from != null ? from : (bd != null ? bd.getDate() : null),
                        to != null ? to : (bd != null ? bd.getDate() : null));

        long totalOrders = periodStats.getTotalCount() != null ? periodStats.getTotalCount() : 0;
        long deliveredOrders = periodStats.getDeliveredCount() != null ? periodStats.getDeliveredCount() : 0;
        long refusedOrders = periodStats.getRefusedCount() != null ? periodStats.getRefusedCount() : 0;
        long cancelledOrders = periodStats.getCancelledCount() != null ? periodStats.getCancelledCount() : 0;
        long deferredOrders = periodStats.getDeferredCount() != null ? periodStats.getDeferredCount() : 0;
        long partialDeliveryOrders = periodStats.getPartialCount() != null ? periodStats.getPartialCount() : 0;
        long inTransitOrders = periodStats.getInTransitCount() != null ? periodStats.getInTransitCount() : 0;
        long waitingOrders = totalOrders - deliveredOrders - refusedOrders - cancelledOrders
                - deferredOrders - partialDeliveryOrders - inTransitOrders;

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
                .deferredOrders(deferredOrders)
                .inTransitOrders(inTransitOrders)
                .waitingOrders(waitingOrders)
                .partialDeliveryOrders(partialDeliveryOrders)
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
                .deferredOrders(summary.getDeferredOrders())
                .partialDeliveryOrders(summary.getOtherOrders())
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

    // =====================================================================
    // 9. تقرير وقت التوصيل (Delivery Time Report)
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'delivery_time_' + #orgId + '_' + #from + '_' + #to")
    public DeliveryTimeReport getDeliveryTimeReport(Long orgId, LocalDate from, LocalDate to) {
        DeliveryTimeReport.DeliveryTimeReportBuilder builder = DeliveryTimeReport.builder();

        // إحصائيات عامة
        List<Object[]> overallStats = orderReportRepository.getDeliveryTimeStatsByOrg(orgId, from, to);
        if (!overallStats.isEmpty() && overallStats.get(0) != null) {
            Object[] row = overallStats.get(0);
            double avgMinutes = row[0] != null ? ((Number) row[0]).doubleValue() : 0;
            double minMinutes = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            double maxMinutes = row[2] != null ? ((Number) row[2]).doubleValue() : 0;

            builder.avgDeliveryTimeHours(Math.round((avgMinutes / 60.0) * 100.0) / 100.0)
                   .avgDeliveryTimeMinutes(Math.round(avgMinutes * 100.0) / 100.0)
                   .fastestDeliveryHours(Math.round((minMinutes / 60.0) * 100.0) / 100.0)
                   .slowestDeliveryHours(Math.round((maxMinutes / 60.0) * 100.0) / 100.0);
        }

        // توزيع فئات الوقت
        List<Object[]> distribution = orderReportRepository.getDeliveryTimeDistribution(orgId, from, to);
        if (!distribution.isEmpty() && distribution.get(0) != null) {
            Object[] row = distribution.get(0);
            builder.within2Hours(row[0] != null ? ((Number) row[0]).longValue() : 0)
                   .within4Hours(row[1] != null ? ((Number) row[1]).longValue() : 0)
                   .within8Hours(row[2] != null ? ((Number) row[2]).longValue() : 0)
                   .within24Hours(row[3] != null ? ((Number) row[3]).longValue() : 0)
                   .moreThan24Hours(row[4] != null ? ((Number) row[4]).longValue() : 0);
        }

        // أداء المناديب
        List<Object[]> courierRows = orderReportRepository.getDeliveryTimeByCourier(orgId, from, to);
        List<DeliveryTimeReport.CourierDeliveryTime> courierTimes = new ArrayList<>();
        int rank = 1;
        for (Object[] row : courierRows) {
            Long courierId = row[0] != null ? ((Number) row[0]).longValue() : null;
            double avgMinutes = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            long totalDelivered = row[2] != null ? ((Number) row[2]).longValue() : 0;

            User courier = courierId != null ? organizationService.getUserById(courierId) : null;
            courierTimes.add(DeliveryTimeReport.CourierDeliveryTime.builder()
                    .courierId(courierId)
                    .courierName(courier != null ? courier.getFullName() : "غير معروف")
                    .avgHours(Math.round((avgMinutes / 60.0) * 100.0) / 100.0)
                    .totalDelivered(totalDelivered)
                    .rank(rank++)
                    .build());
        }
        builder.courierTimes(courierTimes);

        // أداء المحافظات
        List<Object[]> govRows = orderReportRepository.getDeliveryTimeByGovernorate(orgId, from, to);
        List<DeliveryTimeReport.GovernorateDeliveryTime> govTimes = new ArrayList<>();
        for (Object[] row : govRows) {
            String govStr = row[0] != null ? row[0].toString() : null;
            double avgMinutes = row[1] != null ? ((Number) row[1]).doubleValue() : 0;
            long totalDelivered = row[2] != null ? ((Number) row[2]).longValue() : 0;

            Governorate gov = null;
            if (govStr != null) {
                try { gov = Governorate.valueOf(govStr); } catch (Exception ignored) {}
            }
            if (gov != null) {
                govTimes.add(DeliveryTimeReport.GovernorateDeliveryTime.builder()
                        .governorate(gov)
                        .governorateName(gov.getArabicName())
                        .avgHours(Math.round((avgMinutes / 60.0) * 100.0) / 100.0)
                        .totalDelivered(totalDelivered)
                        .build());
            }
        }
        builder.governorateTimes(govTimes);

        return builder.build();
    }

    // =====================================================================
    // 10. مقارنة الفترات (Period Comparison)
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'comparison_' + #orgId + '_' + #from + '_' + #to")
    public PeriodComparisonReport getPeriodComparison(Long orgId, LocalDate from, LocalDate to) {
        // الفترة الحالية
        PeriodReport current = getPeriodReport(orgId, from, to);

        // حساب الفترة السابقة بنفس المدة
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to);
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(daysBetween);
        PeriodReport previous = getPeriodReport(orgId, prevFrom, prevTo);

        // حساب نسب التغيير
        double ordersGrowth = calculateGrowth(previous.getTotalOrders(), current.getTotalOrders());
        double deliveryRateChange = current.getDeliveryRate() - previous.getDeliveryRate();
        double revenueGrowth = calculateGrowthBD(previous.getTotalCollected(), current.getTotalCollected());
        double refusalRateChange = current.getRefusalRate() - previous.getRefusalRate();
        double avgOrderValueChange = calculateGrowthBD(previous.getAvgOrderValue(), current.getAvgOrderValue());

        // تحديد الاتجاه العام
        int positiveIndicators = 0;
        if (ordersGrowth > 0) positiveIndicators++;
        if (deliveryRateChange > 0) positiveIndicators++;
        if (revenueGrowth > 0) positiveIndicators++;
        if (refusalRateChange < 0) positiveIndicators++; // الرفض أقل = إيجابي

        String trend = positiveIndicators >= 3 ? "IMPROVING"
                : positiveIndicators <= 1 ? "DECLINING" : "STABLE";

        return PeriodComparisonReport.builder()
                .currentPeriod(current)
                .previousPeriod(previous)
                .ordersGrowth(Math.round(ordersGrowth * 100.0) / 100.0)
                .deliveryRateChange(Math.round(deliveryRateChange * 100.0) / 100.0)
                .revenueGrowth(Math.round(revenueGrowth * 100.0) / 100.0)
                .refusalRateChange(Math.round(refusalRateChange * 100.0) / 100.0)
                .avgOrderValueChange(Math.round(avgOrderValueChange * 100.0) / 100.0)
                .overallTrend(trend)
                .build();
    }

    // =====================================================================
    // 11. تقرير المرتجعات (Returns Report)
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'returns_' + #orgId + '_' + #from + '_' + #to")
    public ReturnsReport getReturnsReport(Long orgId, LocalDate from, LocalDate to) {
        // إحصائيات الفترة لحساب الإجمالي ونسبة المرتجعات
        PeriodReport period = getPeriodReport(orgId, from, to);

        long totalReturns = period.getRefused() + period.getCancelled() + period.getDeferred();
        double returnRate = period.getTotalOrders() > 0
                ? (totalReturns * 100.0 / period.getTotalOrders()) : 0;

        // توزيع حسب السبب
        List<Object[]> reasonRows = orderReportRepository.getReturnsByReason(orgId, from, to);
        Map<com.shipment.shippinggo.enums.RejectionReason, Long> byReason = new LinkedHashMap<>();
        List<ReturnsReport.ReasonStat> topReasons = new ArrayList<>();

        for (Object[] row : reasonRows) {
            com.shipment.shippinggo.enums.RejectionReason reason =
                    (com.shipment.shippinggo.enums.RejectionReason) row[0];
            long count = ((Number) row[1]).longValue();
            byReason.put(reason, count);
            topReasons.add(ReturnsReport.ReasonStat.builder()
                    .reason(reason)
                    .reasonLabel(reason.getArabicName())
                    .count(count)
                    .percentage(totalReturns > 0 ? Math.round((count * 100.0 / totalReturns) * 100.0) / 100.0 : 0)
                    .build());
        }

        // المرتجعات حسب المندوب
        List<Object[]> courierRows = orderReportRepository.getReturnsByCourier(orgId, from, to);
        List<ReturnsReport.CourierReturnStat> byCourier = new ArrayList<>();
        for (Object[] row : courierRows) {
            Long courierId = ((Number) row[0]).longValue();
            long total = ((Number) row[1]).longValue();
            long returned = ((Number) row[2]).longValue();

            User courier = organizationService.getUserById(courierId);
            byCourier.add(ReturnsReport.CourierReturnStat.builder()
                    .courierId(courierId)
                    .courierName(courier != null ? courier.getFullName() : "غير معروف")
                    .totalOrders(total)
                    .returnedOrders(returned)
                    .returnRate(total > 0 ? Math.round((returned * 100.0 / total) * 100.0) / 100.0 : 0)
                    .build());
        }
        // ترتيب حسب نسبة المرتجعات (أعلى أولاً)
        byCourier.sort((a, b) -> Double.compare(b.getReturnRate(), a.getReturnRate()));

        // المرتجعات حسب المحافظة
        List<Object[]> govRows = orderReportRepository.getReturnsByGovernorate(orgId, from, to);
        Map<Governorate, ReturnsReport.ReturnStat> byGovernorate = new HashMap<>();
        for (Object[] row : govRows) {
            Governorate gov = (Governorate) row[0];
            long total = ((Number) row[1]).longValue();
            long returned = ((Number) row[2]).longValue();
            byGovernorate.put(gov, ReturnsReport.ReturnStat.builder()
                    .totalOrders(total)
                    .returnedOrders(returned)
                    .returnRate(total > 0 ? Math.round((returned * 100.0 / total) * 100.0) / 100.0 : 0)
                    .build());
        }

        return ReturnsReport.builder()
                .totalReturns(totalReturns)
                .refusedCount(period.getRefused())
                .cancelledCount(period.getCancelled())
                .deferredCount(period.getDeferred())
                .returnRate(Math.round(returnRate * 100.0) / 100.0)
                .byReason(byReason)
                .topReasons(topReasons)
                .byCourier(byCourier)
                .byGovernorate(byGovernorate)
                .build();
    }

    // =====================================================================
    // 12. تقرير الرحلات (Trip Report) - صادرة + واردة
    // =====================================================================

    @Cacheable(value = "reportsCache", key = "'trips_' + #orgId + '_' + #from + '_' + #to")
    public TripReport getTripReport(Long orgId, LocalDate from, LocalDate to) {
        TripReport.TripReportBuilder builder = TripReport.builder();

        // عدد الرحلات حسب الحالة
        List<Object[]> statusRows = tripRepository.countTripsByStatus(orgId, from, to);
        long totalTrips = 0;
        for (Object[] row : statusRows) {
            com.shipment.shippinggo.enums.TripStatus status =
                    (com.shipment.shippinggo.enums.TripStatus) row[0];
            long count = ((Number) row[1]).longValue();
            totalTrips += count;
            switch (status) {
                case PREPARING -> builder.preparingTrips(count);
                case IN_TRANSIT -> builder.inTransitTrips(count);
                case ARRIVED -> builder.arrivedTrips(count);
                case COMPLETED -> builder.completedTrips(count);
                case RETURNING -> builder.returningTrips(count);
                case RETURNED -> builder.returnedTrips(count);
                case CANCELLED -> builder.cancelledTrips(count);
            }
        }
        builder.totalTrips(totalTrips);

        // إجمالي الأوردرات المنقولة
        long totalOrdersShipped = tripRepository.countTotalOrdersShipped(orgId, from, to);
        builder.totalOrdersShipped(totalOrdersShipped);
        builder.avgOrdersPerTrip(totalTrips > 0
                ? Math.round((totalOrdersShipped * 1.0 / totalTrips) * 100.0) / 100.0 : 0);

        // أداء الشاحنات
        List<Object[]> vehicleRows = tripRepository.getTripStatsByVehicle(orgId, from, to);
        List<TripReport.VehicleTripStat> byVehicle = new ArrayList<>();
        for (Object[] row : vehicleRows) {
            byVehicle.add(TripReport.VehicleTripStat.builder()
                    .vehicleId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .vehiclePlateNumber(row[1] != null ? row[1].toString() : "")
                    .vehicleType(row[2] != null ? row[2].toString() : "")
                    .totalTrips(row[3] != null ? ((Number) row[3]).longValue() : 0)
                    .totalOrdersShipped(row[4] != null ? ((Number) row[4]).longValue() : 0)
                    .completedTrips(row[5] != null ? ((Number) row[5]).longValue() : 0)
                    .build());
        }
        builder.byVehicle(byVehicle);

        // الرحلات حسب الوجهة (صادرة)
        List<Object[]> destRows = tripRepository.getTripStatsByDestination(orgId, from, to);
        List<TripReport.DestinationTripStat> byDestination = new ArrayList<>();
        for (Object[] row : destRows) {
            byDestination.add(TripReport.DestinationTripStat.builder()
                    .orgId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .orgName(row[1] != null ? row[1].toString() : "")
                    .totalTrips(row[2] != null ? ((Number) row[2]).longValue() : 0)
                    .totalOrders(row[3] != null ? ((Number) row[3]).longValue() : 0)
                    .completedTrips(row[4] != null ? ((Number) row[4]).longValue() : 0)
                    .build());
        }
        builder.byDestination(byDestination);

        // الرحلات حسب المصدر (واردة)
        List<Object[]> originRows = tripRepository.getTripStatsByOrigin(orgId, from, to);
        List<TripReport.DestinationTripStat> byOrigin = new ArrayList<>();
        for (Object[] row : originRows) {
            byOrigin.add(TripReport.DestinationTripStat.builder()
                    .orgId(row[0] != null ? ((Number) row[0]).longValue() : null)
                    .orgName(row[1] != null ? row[1].toString() : "")
                    .totalTrips(row[2] != null ? ((Number) row[2]).longValue() : 0)
                    .totalOrders(row[3] != null ? ((Number) row[3]).longValue() : 0)
                    .completedTrips(row[4] != null ? ((Number) row[4]).longValue() : 0)
                    .build());
        }
        builder.byOrigin(byOrigin);

        // اتجاهات الرحلات يومياً
        List<Object[]> trendRows = tripRepository.getTripTrends(orgId, from, to);
        List<String> trendLabels = new ArrayList<>();
        List<Long> trendCounts = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");
        for (Object[] row : trendRows) {
            LocalDate date = (LocalDate) row[0];
            trendLabels.add(date.format(fmt));
            trendCounts.add(((Number) row[1]).longValue());
        }
        builder.trendLabels(trendLabels).trendCounts(trendCounts);

        return builder.build();
    }

    // =====================================================================
    // === Helper Methods ===
    // =====================================================================

    private double calculateGrowth(long previous, long current) {
        if (previous == 0) return current > 0 ? 100.0 : 0;
        return ((current - previous) * 100.0 / previous);
    }

    private double calculateGrowthBD(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0;
        }
        return current.subtract(previous)
                .multiply(new BigDecimal(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}