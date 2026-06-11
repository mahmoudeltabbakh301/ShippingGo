package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * تقرير مالي شامل يوضح صافي كل اتجاه (صادر/وارد/مناديب) والصافي الكلي
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialSummaryReport {

    private Long businessDayId;
    private String businessDayName;
    private LocalDate fromDate;
    private LocalDate toDate;

    // === إجمالي الأوردرات ===
    private long totalOrders;
    private long deliveredOrders;
    private long refusedOrders;
    private long cancelledOrders;
    private long deferredOrders;
    private long inTransitOrders;
    private long waitingOrders;
    private long partialDeliveryOrders;

    // === عمولات الصادر (عمولات المنظمة من الأوردرات اللي بتبعتها لمنظمات تانية) ===
    @Builder.Default
    private BigDecimal outgoingOrgCommissions = BigDecimal.ZERO;      // إجمالي عمولات صادرة
    @Builder.Default
    private BigDecimal outgoingDeliveryComm = BigDecimal.ZERO;        // عمولات توصيل صادرة
    @Builder.Default
    private BigDecimal outgoingRejectionComm = BigDecimal.ZERO;       // عمولات رفض صادرة
    @Builder.Default
    private BigDecimal outgoingCancellationComm = BigDecimal.ZERO;    // عمولات إلغاء صادرة

    // === عمولات الوارد (عمولات المنظمة من الأوردرات اللي بتستقبلها من منظمات تانية) ===
    @Builder.Default
    private BigDecimal incomingOrgCommissions = BigDecimal.ZERO;      // إجمالي عمولات واردة
    @Builder.Default
    private BigDecimal incomingDeliveryComm = BigDecimal.ZERO;        // عمولات توصيل واردة
    @Builder.Default
    private BigDecimal incomingRejectionComm = BigDecimal.ZERO;       // عمولات رفض واردة
    @Builder.Default
    private BigDecimal incomingCancellationComm = BigDecimal.ZERO;    // عمولات إلغاء واردة

    // === عمولات المناديب (مصروف - اللي بندفعه للمناديب) ===
    @Builder.Default
    private BigDecimal courierCommissions = BigDecimal.ZERO;          // إجمالي عمولات مناديب
    @Builder.Default
    private BigDecimal courierDeliveryComm = BigDecimal.ZERO;         // عمولات توصيل مناديب
    @Builder.Default
    private BigDecimal courierRejectionComm = BigDecimal.ZERO;        // عمولات رفض مناديب
    @Builder.Default
    private BigDecimal courierCancellationComm = BigDecimal.ZERO;     // عمولات إلغاء مناديب

    // === عمولات غير مسندة (أوردرات المنظمة اللي مش مسندة لحد) ===
    @Builder.Default
    private BigDecimal unassignedCommissions = BigDecimal.ZERO;       // إجمالي عمولات غير مسندة
    @Builder.Default
    private BigDecimal unassignedDeliveryComm = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal unassignedRejectionComm = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal unassignedCancellationComm = BigDecimal.ZERO;

    // === الإجماليات والصافي ===
    @Builder.Default
    private BigDecimal totalCommissionRevenue = BigDecimal.ZERO;  // إجمالي إيرادات العمولات (صادر + وارد + غير مسندة)
    @Builder.Default
    private BigDecimal totalOutgoingCommissions = BigDecimal.ZERO; // إجمالي صادر (للتوافق)
    @Builder.Default
    private BigDecimal totalIncomingCommissions = BigDecimal.ZERO; // إجمالي وارد (للتوافق)
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;               // صافي الربح = إيرادات العمولات − عمولات المناديب

    // === تفاصيل حسب المنظمة/المندوب ===
    @Builder.Default
    private List<EntityFinancialDetail> outgoingOrgDetails = new ArrayList<>();
    @Builder.Default
    private List<EntityFinancialDetail> incomingOrgDetails = new ArrayList<>();
    @Builder.Default
    private List<EntityFinancialDetail> courierDetails = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EntityFinancialDetail {
        private Long entityId;
        private String entityName;
        private String entityType; // "organization" or "courier"
        private long totalOrders;
        private long deliveredOrders;
        private long refusedOrders;
        private long cancelledOrders;
        @Builder.Default
        private BigDecimal deliveredAmount = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal deliveryCommission = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal rejectionCommission = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal cancellationCommission = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal totalCommission = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal netAmount = BigDecimal.ZERO;
    }
}
