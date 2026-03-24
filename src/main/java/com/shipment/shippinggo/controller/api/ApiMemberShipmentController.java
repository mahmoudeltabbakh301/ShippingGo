package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.ShipmentRequest;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import com.shipment.shippinggo.repository.ShipmentRequestRepository;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/member")
public class ApiMemberShipmentController {

    private final OrganizationService organizationService;
    private final ShipmentRequestRepository shipmentRequestRepository;
    private final UserService userService;

    public ApiMemberShipmentController(OrganizationService organizationService,
                                       ShipmentRequestRepository shipmentRequestRepository,
                                       UserService userService) {
        this.organizationService = organizationService;
        this.shipmentRequestRepository = shipmentRequestRepository;
        this.userService = userService;
    }

    @GetMapping("/organizations/nearby")
    public ResponseEntity<ApiResponse<List<OrganizationService.OrganizationDistance>>> getNearbyOrganizations(
            @RequestParam double lat,
            @RequestParam double lng,
            @AuthenticationPrincipal User user) {
        
        if (user.getRole() != Role.MEMBER) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only members can search nearby organizations"));
        }

        List<OrganizationService.OrganizationDistance> nearby = organizationService.findNearbyOrganizations(lat, lng);
        return ResponseEntity.ok(ApiResponse.success(nearby));
    }

    @PostMapping("/shipment-requests")
    public ResponseEntity<ApiResponse<ShipmentRequest>> createShipmentRequest(
            @Valid @RequestBody MemberShipmentRequestDto dto,
            @AuthenticationPrincipal User user) {

        if (user.getRole() != Role.MEMBER) {
            return ResponseEntity.status(403).body(ApiResponse.error("Only members can create shipment requests"));
        }

        Organization org = organizationService.getById(dto.getOrganizationId());
        if (org == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Organization not found"));
        }

        ShipmentRequest request = ShipmentRequest.builder()
                .requester(user)
                .organization(org)
                .senderName(dto.getSenderName() != null ? dto.getSenderName() : user.getFullName())
                .senderPhone(dto.getSenderPhone() != null ? dto.getSenderPhone() : user.getPhone())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .recipientAddress(dto.getRecipientAddress())
                .contentDescription(dto.getContentDescription())
                .estimatedAmount(dto.getEstimatedAmount())
                .status(ShipmentRequestStatus.PENDING)
                .build();

        ShipmentRequest savedRequest = shipmentRequestRepository.save(request);
        
        return ResponseEntity.ok(ApiResponse.success(savedRequest, "Shipment request created successfully"));
    }

    @PutMapping("/location")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMemberLocation(
            @RequestBody Map<String, Double> body,
            @AuthenticationPrincipal User user) {

        Double lat = body.get("latitude");
        Double lng = body.get("longitude");

        if (lat == null || lng == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("latitude and longitude are required"));
        }

        userService.updateLocation(user, lat, lng);

        Map<String, Object> result = Map.of(
                "latitude", lat,
                "longitude", lng,
                "message", "Location updated successfully"
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Location updated successfully"));
    }

    @Data
    public static class MemberShipmentRequestDto {
        @NotNull(message = "Organization ID is required")
        private Long organizationId;
        
        private String senderName;
        private String senderPhone;

        @NotBlank(message = "Recipient Name is required")
        private String recipientName;

        @NotBlank(message = "Recipient Phone is required")
        private String recipientPhone;

        @NotBlank(message = "Recipient Address is required")
        private String recipientAddress;

        private String contentDescription;
        private BigDecimal estimatedAmount;
    }
}
