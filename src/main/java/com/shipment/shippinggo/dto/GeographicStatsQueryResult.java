package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.Governorate;
import java.math.BigDecimal;

public interface GeographicStatsQueryResult {
    Governorate getGovernorate();
    Long getTotalOrders();
    Long getDeliveredOrders();
    BigDecimal getTotalRevenue();
}
