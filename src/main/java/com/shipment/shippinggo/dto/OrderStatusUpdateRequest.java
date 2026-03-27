package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderStatusUpdateRequest {
    private OrderStatus status;
    private BigDecimal amount;
    private BigDecimal rejectionPayment;
    private Integer deliveredPieces;
    private BigDecimal partialDeliveryAmount;
    private String notes;
}
