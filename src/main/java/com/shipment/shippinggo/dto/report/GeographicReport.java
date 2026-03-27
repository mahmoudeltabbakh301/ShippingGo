package com.shipment.shippinggo.dto.report;

import com.shipment.shippinggo.enums.Governorate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeographicReport {
    private Map<Governorate, GovernorateStats> statsByGovernorate;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GovernorateStats {
        private long totalOrders;
        private double deliveryRate;
        @Builder.Default
        private BigDecimal totalRevenue = BigDecimal.ZERO;
    }
}
