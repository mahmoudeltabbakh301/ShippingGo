package com.shipment.shippinggo.dto;

import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.Role;
import lombok.Data;

@Data
public class RegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^01[0125][0-9]{8}$", message = "Phone number must be a valid Egyptian number (e.g., 01012345678)")
    private String phone;

    // Organization creation option
    private boolean createOrganization;
    private OrganizationType organizationType;
    private String organizationName;
    private String organizationAddress;
    @Pattern(regexp = "^(01[0125][0-9]{8})?$", message = "Organization phone number must be a valid Egyptian number or empty")
    private String organizationPhone;

    private String organizationEmail;
    private Governorate organizationGovernorate;

    private Governorate governorate;
}
