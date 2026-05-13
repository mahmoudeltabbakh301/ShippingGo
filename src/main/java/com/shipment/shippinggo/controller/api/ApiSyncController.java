package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.*;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API مزامنة تطبيق الديسكتوب مع السيرفر.
 * يعالج العمليات التي تمت offline ويرسلها للسيرفر بالترتيب.
 */
@RestController
@RequestMapping("/api/v1/sync")
public class ApiSyncController {

    private final OrderService orderService;
    private final OrganizationService organizationService;

    public ApiSyncController(OrderService orderService, OrganizationService organizationService) {
        this.orderService = orderService;
        this.organizationService = organizationService;
    }

    /**
     * فحص حالة السيرفر (heartbeat) — يستخدمه الديسكتوب لمراقبة الاتصال.
     */
    @GetMapping("/heartbeat")
    public ResponseEntity<ApiResponse<String>> heartbeat() {
        return ResponseEntity.ok(ApiResponse.success("ok", "Server is alive"));
    }

    /**
     * إنشاء طلب واحد (من الـ offline queue).
     */
    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal User user) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("User does not belong to any organization"));
            }

            Order created = orderService.createOrder(orderDto, user, org);
            Map<String, Object> result = new HashMap<>();
            result.put("serverId", created.getId());
            result.put("code", created.getCode());
            return ResponseEntity.ok(ApiResponse.success(result, "Order created via sync"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * تعديل بيانات طلب موجود.
     */
    @PutMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<String>> updateOrder(
            @PathVariable Long id,
            @RequestBody OrderDto orderDto,
            @AuthenticationPrincipal User user) {
        try {
            orderService.updateOrderDetails(id, orderDto, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Order updated via sync"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * تغيير حالة طلب.
     */
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long id,
            @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal User user) {
        try {
            orderService.updateStatusAdvanced(
                    id,
                    request.getStatus(),
                    request.getAmount(),
                    request.getRejectionPayment(),
                    request.getDeliveredPieces(),
                    request.getPartialDeliveryAmount(),
                    user,
                    request.getNotes());
            return ResponseEntity.ok(ApiResponse.success(null, "Status updated via sync"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * إسناد طلب لمندوب.
     */
    @PutMapping("/orders/{id}/assign-courier")
    public ResponseEntity<ApiResponse<String>> assignCourier(
            @PathVariable Long id,
            @RequestParam Long courierId,
            @AuthenticationPrincipal User user) {
        try {
            orderService.assignToCourier(id, courierId, user);
            return ResponseEntity.ok(ApiResponse.success(null, "Courier assigned via sync"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * تنفيذ مجموعة عمليات مختلطة بالترتيب (batch sync).
     * هذا هو الـ endpoint الرئيسي للمزامنة — يستقبل قائمة من العمليات المختلفة
     * وينفذها بالترتيب مع تتبع النتائج لكل عملية.
     * 
     * يدعم ربط الطلبات المنشأة offline: لو عملية UPDATE_STATUS أو ASSIGN_COURIER
     * مربوطة بطلب تم إنشاؤه في نفس الدفعة، يتم البحث عن الـ serverId
     * من نتائج العمليات السابقة.
     */
    @PostMapping("/actions")
    public ResponseEntity<ApiResponse<SyncBatchResponse>> processBatchActions(
            @RequestBody List<SyncActionRequest> actions,
            @AuthenticationPrincipal User user) {

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("User does not belong to any organization"));
        }

        SyncBatchResponse response = SyncBatchResponse.builder()
                .totalReceived(actions.size())
                .results(new ArrayList<>())
                .build();

        int successCount = 0;
        int failedCount = 0;

        // خريطة لربط localOrderId بالـ serverId للطلبات الجديدة
        Map<Integer, Long> localToServerIdMap = new HashMap<>();

        for (int i = 0; i < actions.size(); i++) {
            SyncActionRequest action = actions.get(i);
            SyncBatchResponse.SyncActionResult result = SyncBatchResponse.SyncActionResult.builder()
                    .index(i)
                    .actionType(action.getActionType())
                    .localOrderId(action.getLocalOrderId())
                    .build();

            try {
                switch (action.getActionType()) {
                    case "CREATE_ORDER" -> {
                        Order created = orderService.createOrder(action.getOrderData(), user, org);
                        result.setServerId(created.getId());
                        result.setSuccess(true);
                        // حفظ الربط للعمليات اللاحقة
                        if (action.getLocalOrderId() != null) {
                            localToServerIdMap.put(action.getLocalOrderId(), created.getId());
                        }
                    }
                    case "UPDATE_ORDER" -> {
                        Long serverId = resolveServerId(action, localToServerIdMap);
                        if (serverId == null) {
                            throw new IllegalArgumentException("Cannot resolve server ID for order update");
                        }
                        orderService.updateOrderDetails(serverId, action.getOrderData(), user);
                        result.setServerId(serverId);
                        result.setSuccess(true);
                    }
                    case "UPDATE_STATUS" -> {
                        Long serverId = resolveServerId(action, localToServerIdMap);
                        if (serverId == null) {
                            throw new IllegalArgumentException("Cannot resolve server ID for status update");
                        }
                        orderService.updateStatusAdvanced(
                                serverId,
                                action.getNewStatus(),
                                action.getAmount(),
                                action.getRejectionPayment(),
                                action.getDeliveredPieces(),
                                action.getPartialDeliveryAmount(),
                                user,
                                action.getNotes());
                        result.setServerId(serverId);
                        result.setSuccess(true);
                    }
                    case "ASSIGN_COURIER" -> {
                        Long serverId = resolveServerId(action, localToServerIdMap);
                        if (serverId == null) {
                            throw new IllegalArgumentException("Cannot resolve server ID for courier assignment");
                        }
                        orderService.assignToCourier(serverId, action.getCourierId(), user);
                        result.setServerId(serverId);
                        result.setSuccess(true);
                    }
                    default -> {
                        result.setSuccess(false);
                        result.setError("Unknown action type: " + action.getActionType());
                    }
                }
            } catch (Exception e) {
                result.setSuccess(false);
                result.setError(e.getMessage());
            }

            if (result.isSuccess()) {
                successCount++;
            } else {
                failedCount++;
            }

            response.getResults().add(result);
        }

        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);

        String message = String.format("Synced %d/%d actions successfully", successCount, actions.size());
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    /**
     * تحديد الـ serverId — إما من الـ action مباشرة أو من خريطة الربط
     * (للطلبات التي تم إنشاؤها في نفس الدفعة).
     */
    private Long resolveServerId(SyncActionRequest action, Map<Integer, Long> localToServerIdMap) {
        if (action.getServerOrderId() != null) {
            return action.getServerOrderId();
        }
        if (action.getLocalOrderId() != null && localToServerIdMap.containsKey(action.getLocalOrderId())) {
            return localToServerIdMap.get(action.getLocalOrderId());
        }
        return null;
    }
}
