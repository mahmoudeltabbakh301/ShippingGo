package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.Governorate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO مخصص لتسجيل الأعضاء من تطبيق الموبايل.
 * لا يحتوي على حقول المنظمة — التسجيل للأعضاء فقط.
 */
@Data
public class MobileRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username must not contain spaces or special characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^01[0125][0-9]{8}$", message = "Phone number must be a valid Egyptian number (e.g., 01012345678)")
    private String phone;

    private Governorate governorate;
}
