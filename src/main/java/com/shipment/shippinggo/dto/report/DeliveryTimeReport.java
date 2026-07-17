package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.shipment.shippinggo.enums.Governorate;
import java.util.List;

/**
 * تقرير وقت التوصيل - يقيس المدة من إسناد المندوب حتى التسليم
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTimeReport {
    private double avgDeliveryTimeHours;
    private double avgDeliveryTimeMinutes;
    private double fastestDeliveryHours;
    private double slowestDeliveryHours;

    // توزيع حسب فئات الوقت
    private long within2Hours;
    private long within4Hours;
    private long within8Hours;
    private long within24Hours;
    private long moreThan24Hours;

    // أداء المناديب حسب السرعة
    private List<CourierDeliveryTime> courierTimes;

    // أداء المحافظات حسب السرعة
    private List<GovernorateDeliveryTime> governorateTimes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CourierDeliveryTime {
        private Long courierId;
        private String courierName;
        private double avgHours;
        private long totalDelivered;
        private int rank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GovernorateDeliveryTime {
        private Governorate governorate;
        private String governorateName;
        private double avgHours;
        private long totalDelivered;
    }
}
