package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.report.*;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    // --- 1. Dashboard Status Report (Current format, updated) ---
    public PeriodReport getDashboardReport(Long orgId, LocalDate from, LocalDate to) {
        return getPeriodReport(orgId, from, to);
    }

    // --- 2. Period Report ---
    public PeriodReport getPeriodReport(Long orgId, LocalDate from, LocalDate to) {
        com.shipment.shippinggo.dto.PeriodStatsQueryResult stats = orderRepository.getPeriodStatsByOwnerOrg(orgId, from, to);
        
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
        BigDecimal avgValue = total > 0 ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return PeriodReport.builder()
                .fromDate(from)
                .toDate(to)
                .totalOrders(total)
                .delivered(delivered)
                .refused(refused)
                .cancelled(cancelled)
                .deferred(deferred)
                .partial(partial)
                .inTransit(inTransit)
                .totalCollected(collected)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();
    }

    // --- 3. Courier Report ---
    public CourierReport getCourierReport(Long courierId, LocalDate from, LocalDate to) {
        com.shipment.shippinggo.dto.PeriodStatsQueryResult statsQuery = orderRepository.getPeriodStatsByCourierId(courierId, from, to);
        
        long total = statsQuery.getTotalCount() != null ? statsQuery.getTotalCount() : 0;
        long delivered = statsQuery.getDeliveredCount() != null ? statsQuery.getDeliveredCount() : 0;
        long refused = statsQuery.getRefusedCount() != null ? statsQuery.getRefusedCount() : 0;
        long cancelled = statsQuery.getCancelledCount() != null ? statsQuery.getCancelledCount() : 0;
        long deferred = statsQuery.getDeferredCount() != null ? statsQuery.getDeferredCount() : 0;
        long partial = statsQuery.getPartialCount() != null ? statsQuery.getPartialCount() : 0;
        long inTransit = statsQuery.getInTransitCount() != null ? statsQuery.getInTransitCount() : 0;
        BigDecimal collected = statsQuery.getTotalCollected() != null ? statsQuery.getTotalCollected() : BigDecimal.ZERO;

        double successRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
        double refRate = total > 0 ? (refused * 100.0 / total) : 0;
        BigDecimal avgValue = total > 0 ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        PeriodReport stats = PeriodReport.builder()
                .fromDate(from)
                .toDate(to)
                .totalOrders(total)
                .delivered(delivered)
                .refused(refused)
                .cancelled(cancelled)
                .deferred(deferred)
                .partial(partial)
                .inTransit(inTransit)
                .totalCollected(collected)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();

        List<com.shipment.shippinggo.dto.GeographicStatsQueryResult> queryResults = orderRepository.getGeographicStatsByCourierId(courierId, from, to);
        Map<Governorate, Long> byGov = new HashMap<>();
        for (com.shipment.shippinggo.dto.GeographicStatsQueryResult result : queryResults) {
            byGov.put(result.getGovernorate(), result.getTotalOrders() != null ? result.getTotalOrders() : 0);
        }

        BigDecimal totalCommission = orderRepository.getTotalCommissionByCourierId(courierId, from, to);

        return CourierReport.builder()
                .periodStats(stats)
                .ordersByGovernorate(byGov)
                .totalCommission(totalCommission)
                .build();
    }

    // --- 4. Organization Advanced Report ---
    public OrganizationReport getOrganizationReport(Long targetOrgId, LocalDate from, LocalDate to) {
        com.shipment.shippinggo.dto.OrganizationInOutStats incoming = orderRepository.getIncomingStatsForOrg(targetOrgId, from, to);
        com.shipment.shippinggo.dto.OrganizationInOutStats outgoing = orderRepository.getOutgoingStatsForOrg(targetOrgId, from, to);

        BigDecimal incAmount = incoming.getTotalCollected() != null ? incoming.getTotalCollected() : BigDecimal.ZERO;
        long incCount = incoming.getTotalOrders() != null ? incoming.getTotalOrders() : 0;

        BigDecimal outAmount = outgoing.getTotalCollected() != null ? outgoing.getTotalCollected() : BigDecimal.ZERO;
        long outCount = outgoing.getTotalOrders() != null ? outgoing.getTotalOrders() : 0;

        return OrganizationReport.builder()
                .incomingOrders(incCount)
                .incomingAmount(incAmount)
                .outgoingOrders(outCount)
                .outgoingAmount(outAmount)
                .netBalance(incAmount.subtract(outAmount)) // simplified logic
                .build();
    }

    // --- 5. Geographic Report ---
    public GeographicReport getGeographicReport(Long orgId, LocalDate from, LocalDate to) {
        List<com.shipment.shippinggo.dto.GeographicStatsQueryResult> queryResults = orderRepository.getGeographicStatsByOwnerOrg(orgId, from, to);

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

    // --- 6. Trends Report (Line Chart) ---
    public TrendData getTrendsReport(Long orgId, LocalDate from, LocalDate to) {
        List<com.shipment.shippinggo.dto.TrendStatsQueryResult> queryResults = orderRepository.getTrendStatsByOwnerOrg(orgId, from, to);

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

    // --- Utility Methods ---

    List<Order> getOrdersInPeriod(Long orgId, LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return orderRepository.findByOwnerOrganizationIdAndOriginalCreationDateBetween(orgId, from, to);
        }
        return orderRepository.findByOwnerOrganizationId(orgId);
    }

    private boolean matchesDate(LocalDate date, LocalDate from, LocalDate to) {
        if (date == null)
            return false;
        return !date.isBefore(from) && !date.isAfter(to);
    }

    private PeriodReport calculatePeriodReport(List<Order> orders, LocalDate from, LocalDate to) {
        long total = orders.size();
        long delivered = 0, refused = 0, cancelled = 0, deferred = 0, partial = 0, inTransit = 0;
        BigDecimal collected = BigDecimal.ZERO;

        for (Order o : orders) {
            switch (o.getStatus()) {
                case DELIVERED:
                    delivered++;
                    break;
                case REFUSED:
                    refused++;
                    break;
                case CANCELLED:
                    cancelled++;
                    break;
                case DEFERRED:
                    deferred++;
                    break;
                case PARTIAL_DELIVERY:
                    partial++;
                    break;
                case IN_TRANSIT:
                    inTransit++;
                    break;
                default:
                    break;
            }
        }

        collected = calculateTotalCollected(orders);

        double successRate = total > 0 ? ((delivered + partial) * 100.0 / total) : 0;
        double refRate = total > 0 ? (refused * 100.0 / total) : 0;

        BigDecimal avgValue = total > 0 ? collected.divide(new BigDecimal(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return PeriodReport.builder()
                .fromDate(from)
                .toDate(to)
                .totalOrders(total)
                .delivered(delivered)
                .refused(refused)
                .cancelled(cancelled)
                .deferred(deferred)
                .partial(partial)
                .inTransit(inTransit)
                .totalCollected(collected)
                .deliveryRate(Math.round(successRate * 100.0) / 100.0)
                .refusalRate(Math.round(refRate * 100.0) / 100.0)
                .avgOrderValue(avgValue)
                .build();
    }

    private BigDecimal calculateTotalCollected(List<Order> orders) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getStatus() == OrderStatus.DELIVERED) {
                sum = sum.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            } else if (o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                sum = sum.add(o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount() : BigDecimal.ZERO);
            }
        }
        return sum;
    }

    // Legacy support for older templates temporarily
    @lombok.Data
    public static class StatusReport {
        private List<String> labels = new ArrayList<>();
        private List<Long> data = new ArrayList<>();
        private BigDecimal totalRevenue = BigDecimal.ZERO;
    }

    public StatusReport getOrganizationStatusReport(Long orgId) {
        List<com.shipment.shippinggo.dto.StatusStatsQueryResult> stats = orderRepository.getStatusStatsByOwnerOrg(orgId);

        long[] counts = new long[OrderStatus.values().length];
        BigDecimal revenue = BigDecimal.ZERO;

        for (com.shipment.shippinggo.dto.StatusStatsQueryResult stat : stats) {
            if (stat.getStatus() != null) {
                counts[stat.getStatus().ordinal()] = stat.getCount() != null ? stat.getCount() : 0;
                revenue = revenue.add(stat.getRevenue() != null ? stat.getRevenue() : BigDecimal.ZERO);
            }
        }

        StatusReport report = new StatusReport();
        for (OrderStatus status : OrderStatus.values()) {
            report.getLabels().add(status.getArabicName());
            report.getData().add(counts[status.ordinal()]);
        }
        report.setTotalRevenue(revenue);
        return report;
    }
}




    
    
    

        

            
                        