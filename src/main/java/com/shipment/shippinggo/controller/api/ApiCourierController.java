package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.UserService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/couriers")
public class ApiCourierController {
    
    private final UserService userService;

    public ApiCourierController(UserService userService) {
        this.userService = userService;
    }

    @Data
    public static class LocationUpdateDto {
        private Double latitude;
        private Double longitude;
    }

    @PostMapping("/location")
    public ResponseEntity<?> updateLocation(@AuthenticationPrincipal User user, @RequestBody LocationUpdateDto dto) {
        if (user == null || user.getRole() != Role.COURIER) {
            return ResponseEntity.status(403).body("Only couriers can update location");
        }
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            return ResponseEntity.badRequest().body("Latitude and longitude are required");
        }
        
        userService.updateLocation(user, dto.getLatitude(), dto.getLongitude());
        return ResponseEntity.ok().build();
    }
}
