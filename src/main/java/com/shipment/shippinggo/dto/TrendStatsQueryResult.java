package com.shipment.shippinggo.dto;

import java.time.LocalDate;
import java.math.BigDecimal;

public interface TrendStatsQueryResult {
    LocalDate getCreationDate();
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
}
