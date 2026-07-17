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
    private long inTransitCount; // this is specifically for "مع المناديب" (assigned to courier)
    private long inTransitStatusCount;
    private long pickedUpCount;
    private long outForDeliveryCount;
    private long ordersAssignedToCourierCount;
    private long deliveredCount;
    private long refusedCount;
    private long cancelledCount;
    private long deferredCount;
    private long partialDeliveryCount;
    private long returnedToSenderCount;
    private long warehouseReturnPendingCount;
    private long warehouseReceiptConfirmedCount;

    // Financial
    @Builder.Default
    private BigDecimal totalDeliveredAmount = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommissions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netMerchantDue = BigDecimal.ZERO;

    // Orders currently with non-virtual organizations
    private long ordersWithOrganizationsCount;

    // Pending alerts
    private long pendingShipmentRequestsCount;
    private long pendingMembershipsCount;
    private long openSupportTicketsCount;
    private long unassignedWaitingOrdersCount;

    // Active operations
    private long activeTripsCount;
    private long ordersInTripsCount;
    private boolean businessDayOpen;

    // Courier stats
    private long activeCouriersCount;
    private long activeOwnedCouriersCount; // Internal couriers
    private long activeExternalCouriersCount;

    // Courier performance list (Only for internal couriers)
    @Builder.Default
    private List<CourierPerformance> courierPerformances = new java.util.ArrayList<>();
    
    // Aggregated performance for external couriers
    private CourierPerformance externalCouriersPerformance;

    // Assigned Organization Stats
    private long activeAssignedOrgsCount;
    @Builder.Default
    private List<OrganizationPerformance> organizationPerformances = new java.util.ArrayList<>();
    private OrganizationPerformance topPerformingOrg;

    // Recent orders
    @Builder.Default
    private List<RecentOrderInfo> recentOrders = new java.util.ArrayList<>();

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
