package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class IncomingAssignmentDTO {
    private Long id;
    private Long referenceId; // orderId or requestId
    private String type; // "ORDER" or "REQUEST"
    private String senderName;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private String contentDescription;
    private BigDecimal estimatedAmount;
    private ShipmentRequestStatus status;
    private LocalDateTime createdAt;

    // Additional fields for Order
    private String code;
    private String companyName;
}
