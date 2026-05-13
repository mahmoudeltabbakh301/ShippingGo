package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * تقرير مقارنة الأداء بين المناديب أو المنظمات
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceComparison {

    private String comparisonType; // "couriers" or "organizations"

    @Builder.Default
    private List<PerformanceEntry> entries = new ArrayList<>();

    // أفضل أداء
    private PerformanceEntry topPerformer;
    // أسوأ أداء
    private PerformanceEntry worstPerformer;

    // المتوسط العام
    private double averageDeliveryRate;
    private double averageOrderCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PerformanceEntry {
        private Long entityId;
        private String entityName;
        private String entityType;  // "courier" or "organization"
        private int rank;

        private long totalOrders;
        private long deliveredOrders;
        private long refusedOrders;
        private long cancelledOrders;
        private long deferredOrders;
        private long partialDeliveryOrders;

        private double deliveryRate;
        private double refusalRate;

        @Builder.Default
        private BigDecimal totalCollected = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal totalCommission = BigDecimal.ZERO;
        @Builder.Default
        private BigDecimal netAmount = BigDecimal.ZERO;
    }
}
