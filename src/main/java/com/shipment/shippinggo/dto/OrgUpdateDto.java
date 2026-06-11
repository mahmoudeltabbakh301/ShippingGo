package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.Governorate;
import lombok.Data;

@Data
public class OrgUpdateDto {
    private String name;
    private String address;
    private String phone;
    private String email;
    private Governorate governorate;
    private String about;
    private String pickupPolicy;
    private String returnPolicy;
    private Integer estimatedDeliveryDays;
    private String paymentTerms;
    private String whatsappNumber;
    private String websiteUrl;
}
