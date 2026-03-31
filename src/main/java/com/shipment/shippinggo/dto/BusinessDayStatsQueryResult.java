package com.shipment.shippinggo.dto;

public interface BusinessDayStatsQueryResult {
    Long getTotalCount();
    Long getDeliveredCount();
    Long getPartialCount();
    Long getWaitingCount();
    Long getInTransitCount();
    Long getCancelledCount();
    Long getRefusedCount();
    Long getDeferredCount();
    java.math.BigDecimal getTotalAmount();
    java.math.BigDecimal getDeliveredAmount();
}
