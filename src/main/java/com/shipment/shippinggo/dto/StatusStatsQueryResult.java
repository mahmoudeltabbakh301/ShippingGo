package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.OrderStatus;
import java.math.BigDecimal;

public interface StatusStatsQueryResult {
    OrderStatus getStatus();
    Long getCount();
    BigDecimal getRevenue();
}
