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

    // === المبالغ المحصّلة ===
    @Builder.Default
    private BigDecimal totalOrderAmount = BigDecimal.ZERO;       // إجمالي مبالغ كل الأوردرات
    @Builder.Default
    private BigDecimal deliveredAmount = BigDecimal.ZERO;         // مبلغ المسلم (كامل + جزئي)
    @Builder.Default
    private BigDecimal rejectionPayments = BigDecimal.ZERO;       // مدفوعات الرفض
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;          // إجمالي المحصّل = مسلم + رفض مدفوع

    // === العمولات الصادرة (المنظمة تدفعها لآخرين) ===
    @Builder.Default
    private BigDecimal outgoingOrgCommissions = BigDecimal.ZERO;  // عمولات منظمات صادرة
    @Builder.Default
    private BigDecimal courierCommissions = BigDecimal.ZERO;      // عمولات مناديب
    @Builder.Default
    private BigDecimal totalOutgoingCommissions = BigDecimal.ZERO; // إجمالي صادر

    // === العمولات الواردة (المنظمة تحصلها من آخرين) ===
    @Builder.Default
    private BigDecimal incomingOrgCommissions = BigDecimal.ZERO;  // عمولات المنظمات الواردة
    @Builder.Default
    private BigDecimal totalIncomingCommissions = BigDecimal.ZERO; // إجمالي وارد

    // === الصافي ===
    @Builder.Default
    private BigDecimal netOutgoing = BigDecimal.ZERO;    // صافي الصادر (محصّل - عمولات صادرة)
    @Builder.Default
    private BigDecimal netIncoming = BigDecimal.ZERO;    // صافي الوارد (عمولات واردة)
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;      // الصافي الكلي = وارد - صادر

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
