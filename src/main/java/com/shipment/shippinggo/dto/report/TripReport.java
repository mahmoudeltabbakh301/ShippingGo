package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * تقرير الرحلات - صادرة وواردة لكل منظمة
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripReport {
    private long totalTrips;

    // توزيع حسب الحالة
    private long preparingTrips;
    private long inTransitTrips;
    private long arrivedTrips;
    private long completedTrips;
    private long returningTrips;
    private long returnedTrips;
    private long cancelledTrips;

    // إحصائيات الأوردرات
    private long totalOrdersShipped;
    private double avgOrdersPerTrip;

    // أداء الشاحنات
    private List<VehicleTripStat> byVehicle;

    // الرحلات حسب الوجهة/المصدر
    private List<DestinationTripStat> byDestination;
    private List<DestinationTripStat> byOrigin;

    // اتجاهات الرحلات عبر الزمن
    private List<String> trendLabels;
    private List<Long> trendCounts;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleTripStat {
        private Long vehicleId;
        private String vehiclePlateNumber;
        private String vehicleType;
        private long totalTrips;
        private long totalOrdersShipped;
        private long completedTrips;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DestinationTripStat {
        private Long orgId;
        private String orgName;
        private long totalTrips;
        private long totalOrders;
        private long completedTrips;
    }
}
