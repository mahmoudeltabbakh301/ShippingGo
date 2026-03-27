package com.shipment.shippinggo.dto;

public interface CourierStatsQueryResult {
    Long getTotalCount();

    Long getDeliveredCount();

    Long getRefusedCount();

    Long getCancelledCount();

    Long getInTransitCount();

    java.math.BigDecimal getDeliveredAmount();
}
