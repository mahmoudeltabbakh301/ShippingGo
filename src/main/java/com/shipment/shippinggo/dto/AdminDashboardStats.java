package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStats {

    // Order counts
    private long totalOrders;
    private long ownedOrdersCount;
    private long assignedOrdersCount;
    private long waitingCount;
    private long inTransitCount;
    private long deliveredCount;
    private long refusedCount;
    private long cancelledCount;
    private long deferredCount;
    private long partialDeliveryCount;

    // Financial
    private BigDecimal totalDeliveredAmount;

    // Courier stats
    private long activeCouriersCount;
    private long activeOwnedCouriersCount; // Internal couriers
    private long activeExternalCouriersCount;

    // Courier performance list (Only for internal couriers)
    private List<CourierPerformance> courierPerformances;
    
    // Aggregated performance for external couriers
    private CourierPerformance externalCouriersPerformance;

    // Assigned Organization Stats
    private long activeAssignedOrgsCount;
    private List<OrganizationPerformance> organizationPerformances;
    private OrganizationPerformance topPerformingOrg;

    // Recent orders
    private List<RecentOrderInfo> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourierPerformance {
        private Long courierId;
        private String courierName;
        private long totalOrders;
        private long deliveredOrders;
        private long refusedOrders;
        private double deliveryRate; // percentage
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationPerformance {
        private Long organizationId;
        private String organizationName;
        private String organizationType;
        private long totalOrders;
        private long deliveredOrders;
        private long refusedOrders;
        private double deliveryRate; // percentage
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrderInfo {
        private Long orderId;
        private String code;
        private String recipientName;
        private String status;
        private String statusArabic;
        private String courierName;
        private BigDecimal amount;
        private boolean isAssigned; // True if not owned by the org but assigned to it
    }
}
