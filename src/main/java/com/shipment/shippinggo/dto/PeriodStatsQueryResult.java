package com.shipment.shippinggo.dto;

import java.math.BigDecimal;

public interface PeriodStatsQueryResult {
    Long getTotalCount();
    Long getDeliveredCount();
    Long getRefusedCount();
    Long getCancelledCount();
    Long getDeferredCount();
    Long getPartialCount();
    Long getInTransitCount();
    BigDecimal getTotalCollected();
}
