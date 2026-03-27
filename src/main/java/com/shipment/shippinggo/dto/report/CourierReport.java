package com.shipment.shippinggo.dto.report;

import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierReport {
    private User courier;
    private PeriodReport periodStats;
    
    @Builder.Default
    private BigDecimal totalCommission = BigDecimal.ZERO;
    
    private double avgDeliveryTimeHours;
    private Map<Governorate, Long> ordersByGovernorate;
    private int rank;
}
