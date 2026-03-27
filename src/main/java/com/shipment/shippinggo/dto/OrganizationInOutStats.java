package com.shipment.shippinggo.dto;

import java.math.BigDecimal;

public interface OrganizationInOutStats {
    Long getTotalOrders();
    BigDecimal getTotalCollected();
}
