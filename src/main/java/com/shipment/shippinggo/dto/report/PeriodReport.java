package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodReport {
    private LocalDate fromDate;
    private LocalDate toDate;
    
    private long totalOrders;
    private long delivered;
    private long refused;
    private long cancelled;
    private long deferred;
    private long partial;
    private long inTransit;
    
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommissions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;
    
    private double deliveryRate;
    private double refusalRate;
    
    @Builder.Default
    private BigDecimal avgOrderValue = BigDecimal.ZERO;
}
