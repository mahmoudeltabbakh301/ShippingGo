package com.shipment.shippinggo.dto.report;

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
public class FinancialReport {
    @Builder.Default
    private BigDecimal totalCollected = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalCommissions = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal netProfit = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal unpaidInvoices = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal paidInvoices = BigDecimal.ZERO;
    
    private Map<String, BigDecimal> courierCommissions;
    private Map<String, BigDecimal> orgCommissions;
}
