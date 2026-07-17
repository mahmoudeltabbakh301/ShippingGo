package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * تقرير مقارنة فترتين زمنيتين - يقارن المؤشرات الحالية بالفترة السابقة
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodComparisonReport {
    private PeriodReport currentPeriod;
    private PeriodReport previousPeriod;

    // نسب التغيير (%)
    private double ordersGrowth;
    private double deliveryRateChange;
    private double revenueGrowth;
    private double refusalRateChange;
    private double avgOrderValueChange;

    // الاتجاه العام: IMPROVING, DECLINING, STABLE
    private String overallTrend;
}
