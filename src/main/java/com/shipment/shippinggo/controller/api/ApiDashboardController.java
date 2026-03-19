package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.entity.CourierDayLog;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.CourierDayLogService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/dashboard")
public class ApiDashboardController {

    private final OrganizationService organizationService;
    private final OrderService orderService;
    private final CourierDayLogService courierDayLogService;

    public ApiDashboardController(OrganizationService organizationService, OrderService orderService,
            CourierDayLogService courierDayLogService) {
        this.organizationService = organizationService;
        this.orderService = orderService;
        this.courierDayLogService = courierDayLogService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(@AuthenticationPrincipal User user) {
        Map<String, Object> data = new HashMap<>();

        if (user.getRole() == Role.COURIER) {
            // Courier Dashboard - Get orders assigned to this courier TODAY for the daily
            // reset
            java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
            java.time.LocalDateTime endOfDay = java.time.LocalDate.now().plusDays(1).atStartOfDay();

            List<Order> todayOrders = orderService.getOrdersAssignedToCourierByDate(user.getId(),
                    java.time.LocalDate.now());
            data.put("orders", todayOrders);

            // Compute stats directly from today's orders
            int totalOrders = todayOrders.size();
            int inTransitCount = (int) todayOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.WAITING || o.getStatus() == OrderStatus.IN_TRANSIT)
                    .count();
            int deliveredCount = (int) todayOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED
                            || o.getStatus() == OrderStatus.PARTIAL_DELIVERY)
                    .count();
            int returnedCount = (int) todayOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.REFUSED
                            || o.getStatus() == OrderStatus.CANCELLED
                            || o.getStatus() == OrderStatus.DEFERRED)
                    .count();

            double collectedMoney = todayOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.DELIVERED
                            || o.getStatus() == OrderStatus.PARTIAL_DELIVERY
                            || o.getStatus() == OrderStatus.REFUSED)
                    .mapToDouble(o -> {
                        if (o.getStatus() == OrderStatus.REFUSED) {
                            return o.getRejectionPayment() != null ? o.getRejectionPayment().doubleValue() : 0.0;
                        }
                        if (o.getCollectedAmount() != null) {
                            return o.getCollectedAmount().doubleValue();
                        }
                        if (o.getStatus() == OrderStatus.PARTIAL_DELIVERY && o.getPartialDeliveryAmount() != null) {
                            return o.getPartialDeliveryAmount().doubleValue();
                        }
                        return o.getAmount() != null ? o.getAmount().doubleValue() : 0.0;
                    })
                    .sum();

            data.put("totalOrders", totalOrders);
            data.put("inTransitCount", inTransitCount);
            data.put("deliveredCount", deliveredCount);
            data.put("returnedCount", returnedCount);
            data.put("collectedMoney", collectedMoney);

            // Update day log for record keeping with the exact values displayed on dashboard
            courierDayLogService.updateDayLogStats(
                user, 
                totalOrders, 
                deliveredCount, 
                returnedCount, 
                (int) todayOrders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count(),
                inTransitCount,
                java.math.BigDecimal.valueOf(collectedMoney)
            );

        } else if (user.getRole() == Role.MEMBER) {
            List<Order> memberOrders = orderService.getOrdersByRecipientPhone(user.getPhone());
            data.put("orders", memberOrders);
        } else {
            // Admin / Manager / Org Role Logic
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("User has no organization"));
            }

            data.put("organization", org);
            data.put("orders", orderService.getOrdersByOrganization(org.getId()));

            if (user.getRole() == Role.ADMIN || user.getRole() == Role.DATA_ENTRY) {
                data.put("pendingMemberships", organizationService.getPendingMemberships(org.getId()));
                if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
                    data.put("sharedOffices", organizationService.getOfficesByCompany(org.getId()));
                } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
                    data.put("sharedCompanies", organizationService.getCompaniesByStore(org.getId()));
                } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                    data.put("sharedCompanies", organizationService.getCompaniesByOffice(org.getId()));
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/courier/history")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<CourierDayLog>>> getCourierHistory(@AuthenticationPrincipal User user) {
        List<CourierDayLog> dayLogs = courierDayLogService.getDayLogs(user.getId());
        return ResponseEntity.ok(ApiResponse.success(dayLogs));
    }

    @GetMapping("/courier/day/{date}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourierDay(
            @AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Map<String, Object> data = new HashMap<>();
        List<Order> orders = orderService.getOrdersAssignedToCourierByDate(user.getId(), date);
        data.put("orders", orders);

        Optional<CourierDayLog> dayLog = courierDayLogService.getDayLog(user.getId(), date);
        dayLog.ifPresent(log -> data.put("dayLog", log));

        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
