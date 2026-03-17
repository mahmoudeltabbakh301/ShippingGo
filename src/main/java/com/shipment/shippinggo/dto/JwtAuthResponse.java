package com.shipment.shippinggo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String role;
        private String governorate;
        private String profilePicture;
    }
}
