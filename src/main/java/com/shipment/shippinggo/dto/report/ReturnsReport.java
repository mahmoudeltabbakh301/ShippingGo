package com.shipment.shippinggo.dto.report;

import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.RejectionReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * تقرير المرتجعات التفصيلي - يشمل أسباب الرفض والتحليلات
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnsReport {
    private long totalReturns;
    private long refusedCount;
    private long cancelledCount;
    private long deferredCount;
    private double returnRate;

    // توزيع حسب السبب
    private Map<RejectionReason, Long> byReason;

    // أكثر الأسباب تكراراً
    private List<ReasonStat> topReasons;

    // المرتجعات حسب المندوب
    private List<CourierReturnStat> byCourier;

    // المرتجعات حسب المحافظة
    private Map<Governorate, ReturnStat> byGovernorate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReasonStat {
        private RejectionReason reason;
        private String reasonLabel;
        private long count;
        private double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierReturnStat {
        private Long courierId;
        private String courierName;
        private long totalOrders;
        private long returnedOrders;
        private double returnRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnStat {
        private long totalOrders;
        private long returnedOrders;
        private double returnRate;
    }
}
