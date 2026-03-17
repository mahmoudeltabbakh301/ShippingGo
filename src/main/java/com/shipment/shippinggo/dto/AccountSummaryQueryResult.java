package com.shipment.shippinggo.dto;

import java.math.BigDecimal;

public interface AccountSummaryQueryResult {
    Long getTotalOrders();

    Long getDeliveredOrders();

    Long getRefusedOrders();

    BigDecimal getTotalAmount();

    BigDecimal getDeliveredAmount();
}
