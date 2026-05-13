package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * تقرير منظمة تفصيلي يشمل العمولات المفصلة والصافي لكل اتجاه
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationReport {
    private Long organizationId;
    private String organizationName;
    private String direction; // INCOMING or OUTGOING

    // === الأوردرات ===
    private long totalOrders;
    private long deliveredOrders;
    private long refusedOrders;
    private long cancelledOrders;
    private long deferredOrders;
    private long partialDeliveryOrders;

    // === المبالغ ===
    @Builder.Default
    private BigDecimal totalOrderAmount = BigDecimal.ZERO;    // إجمالي مبالغ الأوردرات
    @Builder.Default
    private BigDecimal deliveredAmount = BigDecimal.ZERO;      // المبلغ المسلم (كامل + جزئي)
    @Builder.Default
    private BigDecimal rejectionPayments = BigDecimal.ZERO;    // مدفوعات الرفض

    // === العمولات ===
    @Builder.Default
    private BigDecimal deliveryCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal rejectionCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cancellationCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;

    // === الصافي ===
    @Builder.Default
    private BigDecimal netAmount = BigDecimal.ZERO;            // الصافي بعد خصم العمولة

    // === الأداء ===
    private double deliveryRate;
    private double refusalRate;
}
