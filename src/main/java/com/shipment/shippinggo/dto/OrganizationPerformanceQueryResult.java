package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.OrganizationType;

public interface OrganizationPerformanceQueryResult {
    Long getOrganizationId();
    String getOrganizationName();
    OrganizationType getOrganizationType();
    Long getTotalOrders();
    Long getDeliveredOrders();
    Long getRefusedOrders();
}
