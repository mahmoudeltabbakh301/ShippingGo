package com.shipment.shippinggo.dto;

public interface CourierPerformanceQueryResult {
    Long getCourierId();
    String getCourierFullName();
    String getCourierUsername();
    Long getTotalOrders();
    Long getDeliveredOrders();
    Long getRefusedOrders();
}
