package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class ApiOrderController {

    private final OrderService orderService;
    private final OrganizationService organizationService;

    public ApiOrderController(OrderService orderService, OrganizationService organizationService) {
        this.orderService = orderService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Order>>> listOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long officeId,
            @RequestParam(required = false) OrderStatus status) {

        List<Order> orders;

        if (user.getRole() == Role.COURIER) {
            orders = orderService.getOrdersAssignedToCourier(user.getId());
        } else if (user.getRole() == Role.MEMBER) {
            orders = orderService.getOrdersByRecipientPhone(user.getPhone());
        } else {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("User does not belong to any organization"));
            }

            if (businessDayId != null) {
                orders = orderService.getOrdersByBusinessDayWithFilters(businessDayId, search, code, courierId,
                        officeId, status, null);
            } else {
                orders = orderService.filterOrders(org.getId(), code, courierId, officeId, status, null);
            }
        }

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/track")
    public ResponseEntity<ApiResponse<Order>> trackOrder(
            @RequestParam String code,
            @AuthenticationPrincipal User user) {
        try {
            Order order = orderService.getOrderByCode(code);
            if (order == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Order not found"));
            }
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
        try {
            Order order = orderService.getById(id);
            if (!orderService.canUserAccessOrder(user, order)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal User user) {
        try {
            if (user.getRole() == Role.COURIER) {
                return ResponseEntity.status(403).body(ApiResponse.error("Couriers cannot create orders"));
            }

            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("You must be part of an organization to create orders"));
            }

            Order createdOrder = orderService.createOrder(orderDto, user, org);
            return ResponseEntity.ok(ApiResponse.success(createdOrder, "Order created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long id,
            @RequestBody com.shipment.shippinggo.dto.OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User user) {

        try {
            BigDecimal pda = request.getPartialDeliveryAmount();
            if (request.getStatus() == com.shipment.shippinggo.enums.OrderStatus.PARTIAL_DELIVERY && pda == null) {
                pda = request.getAmount();
            }

            orderService.updateStatusAdvanced(
                    id, 
                    request.getStatus(), 
                    request.getAmount(), 
                    request.getRejectionPayment(), 
                    request.getDeliveredPieces(),
                    pda, 
                    user, 
                    request.getNotes());
            return ResponseEntity.ok(ApiResponse.success(null, "Status updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/assign-courier")
    public ResponseEntity<ApiResponse<String>> assignToCourier(
            @PathVariable Long id,
            @RequestParam Long courierId,
            @AuthenticationPrincipal User user) {
        try {
            orderService.assignToCourier(id, courierId, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Order assigned to courier"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/unassign-courier")
    public ResponseEntity<ApiResponse<String>> unassignCourier(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        try {
            orderService.unassignCourier(id, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Order unassigned from courier"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/assign-organization")
    public ResponseEntity<ApiResponse<String>> assignToOrganization(
            @PathVariable Long id,
            @RequestParam Long organizationId,
            @AuthenticationPrincipal User user) {
        try {
            orderService.assignToOrganization(id, organizationId, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Order assigned to organization"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/unassign-organization")
    public ResponseEntity<ApiResponse<String>> unassignOrganization(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        try {
            orderService.unassignOrganization(id, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Order unassigned from organization"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
