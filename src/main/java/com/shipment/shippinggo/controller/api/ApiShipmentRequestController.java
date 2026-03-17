package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.ShipmentRequest;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import com.shipment.shippinggo.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipment-requests")
public class ApiShipmentRequestController {

    private final OrderService orderService;

    public ApiShipmentRequestController(OrderService orderService) {
        this.orderService = orderService;
        // In the original app, OrderService handles acceptAssignment and
        // rejectAssignment for requests
    }

    @PutMapping("/{orderId}/accept")
    public ResponseEntity<ApiResponse<String>> acceptRequest(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        try {
            orderService.acceptAssignment(orderId, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Shipment request accepted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{orderId}/reject")
    public ResponseEntity<ApiResponse<String>> rejectRequest(
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        try {
            orderService.rejectAssignment(orderId, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Shipment request rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
