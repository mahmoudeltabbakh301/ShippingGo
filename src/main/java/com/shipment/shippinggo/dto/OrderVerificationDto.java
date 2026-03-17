package com.shipment.shippinggo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderVerificationDto {
    private boolean found;
    private String code;
    private String status;
    private String companyName;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private String ownerOrganization;
    private String assignedToOrganization;
    private String message;
}
