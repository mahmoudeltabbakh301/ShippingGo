package com.shipment.shippinggo.dto.report;

import com.shipment.shippinggo.enums.Governorate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * تقرير مندوب تفصيلي يشمل العمولات المفصلة والمبلغ المطلوب تسليمه
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierReport {
    private Long courierId;
    private String courierName;

    private PeriodReport periodStats;

    // === العمولات المفصلة ===
    @Builder.Default
    private BigDecimal deliveryCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal rejectionCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cancellationCommission = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;

    // === المبالغ المحصّلة ===
    @Builder.Default
    private BigDecimal collectedDelivered = BigDecimal.ZERO;      // محصّل من التسليم
    @Builder.Default
    private BigDecimal collectedPartial = BigDecimal.ZERO;         // محصّل من الجزئي
    @Builder.Default
    private BigDecimal collectedRejection = BigDecimal.ZERO;       // محصّل من الرفض
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;           // إجمالي المحصّل

    // === المبلغ المطلوب تسليمه ===
    @Builder.Default
    private BigDecimal amountDue = BigDecimal.ZERO;                // المطلوب = محصّل - عمولات

    // === الأداء ===
    private double deliveryRate;
    private double refusalRate;

    private Map<Governorate, Long> ordersByGovernorate;
    private int rank;
}
