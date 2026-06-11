package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrganizationType;
import lombok.Data;

@Data
public class OrgCreateDto {
    private String name;
    private String address;
    private String phone;
    private String email;
    private OrganizationType type;
    private Governorate governorate;
    private String about;
    private String adminEmail; // ربط بمستخدم موجود (اختياري)
}
