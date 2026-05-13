package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.WebhookOrderDto;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.StoreIntegration;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    // ============================
    // استقبال الأوردرات (Inbound)
    // ============================

    /**
     * استقبال أوردر واحد من منصة خارجية
     *
     * POST /api/webhooks/orders
     * Header: X-Api-Key: {webhookApiKey}
     */
    @PostMapping("/orders")
    public ResponseEntity<?> receiveOrder(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody WebhookOrderDto orderDto) {

        // التحقق من وجود API Key
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "API Key is required in X-Api-Key header"));
        }

        // التحقق من صحة API Key
        Optional<StoreIntegration> integrationOpt = webhookService.validateApiKey(apiKey);
        if (integrationOpt.isEmpty()) {
            log.warn("محاولة Webhook برقم API Key غير صالح: {}", apiKey.substring(0, Math.min(8, apiKey.length())) + "...");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or inactive API Key"));
        }

        // التحقق من البيانات الأساسية
        if (orderDto.getRecipientName() == null || orderDto.getRecipientName().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "recipientName is required"));
        }

        try {
            StoreIntegration integration = integrationOpt.get();
            Order order = webhookService.processWebhookOrder(integration, orderDto);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "تم استقبال الطلب بنجاح",
                    "orderCode", order.getCode(),
                    "orderId", order.getId()));
        } catch (Exception e) {
            log.error("خطأ في معالجة Webhook order: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "خطأ في معالجة الطلب: " + e.getMessage()));
        }
    }

    /**
     * استقبال أوردرات متعددة دفعة واحدة
     *
     * POST /api/webhooks/orders/batch
     * Header: X-Api-Key: {webhookApiKey}
     */
    @PostMapping("/orders/batch")
    public ResponseEntity<?> receiveOrdersBatch(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @RequestBody List<WebhookOrderDto> orders) {

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "API Key is required in X-Api-Key header"));
        }

        Optional<StoreIntegration> integrationOpt = webhookService.validateApiKey(apiKey);
        if (integrationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or inactive API Key"));
        }

        if (orders == null || orders.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "يجب إرسال قائمة طلبات غير فارغة"));
        }

        try {
            StoreIntegration integration = integrationOpt.get();
            List<Order> createdOrders = webhookService.processWebhookOrdersBatch(integration, orders);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "تم استقبال " + createdOrders.size() + " طلب بنجاح",
                    "totalReceived", orders.size(),
                    "totalCreated", createdOrders.size()));
        } catch (Exception e) {
            log.error("خطأ في معالجة Webhook batch: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "خطأ في معالجة الطلبات: " + e.getMessage()));
        }
    }

    // ============================
    // تتبع وإدارة الأوردرات
    // ============================

    /**
     * سحب حالة أوردر بالرقم الخارجي
     *
     * GET /api/webhooks/orders/{externalOrderId}/status
     * Header: X-Api-Key: {webhookApiKey}
     */
    @GetMapping("/orders/{externalOrderId}/status")
    public ResponseEntity<?> getOrderStatus(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @PathVariable String externalOrderId) {

        ResponseEntity<?> authError = validateApiKeyHeader(apiKey);
        if (authError != null) return authError;

        Optional<StoreIntegration> integrationOpt = webhookService.validateApiKey(apiKey);
        if (integrationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or inactive API Key"));
        }

        try {
            Order order = webhookService.getOrderByExternalId(integrationOpt.get(), externalOrderId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("externalOrderId", order.getExternalOrderId());
            result.put("orderCode", order.getCode());
            result.put("status", order.getStatus().name());
            result.put("statusArabic", order.getStatus().getArabicName());
            result.put("statusEnglish", order.getStatus().getEnglishName());
            result.put("recipientName", order.getRecipientName());
            result.put("recipientPhone", order.getRecipientPhone());
            result.put("amount", order.getAmount());
            result.put("collectedAmount", order.getCollectedAmount());
            result.put("rejectionPayment", order.getRejectionPayment());
            result.put("partialDeliveryAmount", order.getPartialDeliveryAmount());
            result.put("deliveredPieces", order.getDeliveredPieces());
            result.put("assignedToOrganization", order.getAssignedToOrganization() != null
                    ? order.getAssignedToOrganization().getName() : null);
            result.put("assignedToCourier", order.getAssignedToCourier() != null
                    ? order.getAssignedToCourier().getFullName() : null);
            result.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
            result.put("updatedAt", order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : null);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * إلغاء أوردر من النظام الخارجي
     * الشرط: لا يمكن الإلغاء إذا كان مسنداً لمندوب
     *
     * POST /api/webhooks/orders/{externalOrderId}/cancel
     * Header: X-Api-Key: {webhookApiKey}
     */
    @PostMapping("/orders/{externalOrderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey,
            @PathVariable String externalOrderId) {

        ResponseEntity<?> authError = validateApiKeyHeader(apiKey);
        if (authError != null) return authError;

        Optional<StoreIntegration> integrationOpt = webhookService.validateApiKey(apiKey);
        if (integrationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid or inactive API Key"));
        }

        try {
            Order order = webhookService.cancelOrderByExternalId(integrationOpt.get(), externalOrderId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "تم إلغاء الطلب بنجاح",
                    "orderCode", order.getCode(),
                    "externalOrderId", externalOrderId,
                    "status", order.getStatus().name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ============================
    // بيانات مرجعية (Reference Data)
    // ============================

    /**
     * قائمة المحافظات المتاحة
     *
     * GET /api/webhooks/governorates
     */
    @GetMapping("/governorates")
    public ResponseEntity<?> getGovernorates() {
        List<Map<String, String>> governorates = new ArrayList<>();
        for (Governorate g : Governorate.values()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("code", g.name());
            entry.put("nameArabic", g.getArabicName());
            entry.put("nameEnglish", g.getEnglishName());
            governorates.add(entry);
        }
        return ResponseEntity.ok(Map.of("success", true, "governorates", governorates));
    }

    /**
     * قائمة حالات الأوردر المتاحة
     *
     * GET /api/webhooks/statuses
     */
    @GetMapping("/statuses")
    public ResponseEntity<?> getStatuses() {
        List<Map<String, String>> statuses = new ArrayList<>();
        for (OrderStatus s : OrderStatus.values()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("code", s.name());
            entry.put("nameArabic", s.getArabicName());
            entry.put("nameEnglish", s.getEnglishName());
            statuses.add(entry);
        }
        return ResponseEntity.ok(Map.of("success", true, "statuses", statuses));
    }

    // ============================
    // Helper Methods
    // ============================

    private ResponseEntity<?> validateApiKeyHeader(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "API Key is required in X-Api-Key header"));
        }
        return null;
    }
}
