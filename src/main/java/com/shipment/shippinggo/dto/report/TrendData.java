package com.shipment.shippinggo.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrendData {
    private List<String> labels; // Dates or periods
    private List<Long> orderCounts;
    private List<BigDecimal> revenues;
    private List<Double> deliveryRates;
}
