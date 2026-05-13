package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.StoreIntegration;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import com.shipment.shippinggo.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * خدمة إرسال تحديثات الحالات للأنظمة الخارجية (Outbound Webhook)
 * تُستدعى تلقائياً عند تغيير حالة أي أوردر مصدره نظام خارجي
 */
@Service
public class OutboundWebhookService {

    private static final Logger log = LoggerFactory.getLogger(OutboundWebhookService.class);
    private static final int TIMEOUT_SECONDS = 10;

    private final StoreIntegrationService storeIntegrationService;
    private final HttpClient httpClient;

    public OutboundWebhookService(StoreIntegrationService storeIntegrationService) {
        this.storeIntegrationService = storeIntegrationService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * إرسال تحديث الحالة للأنظمة الخارجية المرتبطة
     * يتم تنفيذها بشكل غير متزامن حتى لا تبطئ تحديث الحالة الأصلي
     */
    @Async
    public void sendStatusUpdate(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        if (order == null || order.getExternalOrderId() == null) {
            return;
        }

        // جلب الربطات التي لها Callback URL من المنظمة المنشئة
        Long creatorOrgId = order.getCreatorOrganization() != null ? order.getCreatorOrganization().getId() : null;
        if (creatorOrgId == null) {
            return;
        }

        List<StoreIntegration> integrations = storeIntegrationService.getActiveIntegrationsWithCallback(creatorOrgId);
        if (integrations.isEmpty()) {
            return;
        }

        // تجهيز الـ JSON Payload
        String payload = buildStatusUpdatePayload(order, previousStatus, newStatus);

        for (StoreIntegration integration : integrations) {
            if (integration.getCallbackUrl() == null || integration.getCallbackUrl().isBlank()) {
                continue;
            }

            try {
                sendCallback(integration, payload);
            } catch (Exception e) {
                log.error("فشل إرسال Outbound Webhook إلى {} للأوردر {}: {}",
                        integration.getCallbackUrl(), order.getCode(), e.getMessage());
            }
        }
    }

    /**
     * إرسال HTTP POST للـ Callback URL
     */
    private void sendCallback(StoreIntegration integration, String payload) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(integration.getCallbackUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "ShippingGo-Webhook/1.0")
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            // إضافة المفتاح السري للتحقق من الهوية
            if (integration.getCallbackSecret() != null) {
                requestBuilder.header("X-Webhook-Secret", integration.getCallbackSecret());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("تم إرسال Outbound Webhook بنجاح إلى {} (HTTP {})",
                        integration.getCallbackUrl(), response.statusCode());
            } else {
                log.warn("Outbound Webhook رد بـ HTTP {} من {}: {}",
                        response.statusCode(), integration.getCallbackUrl(), response.body());
            }
        } catch (java.io.IOException e) {
            log.error("خطأ شبكة في إرسال Outbound Webhook إلى {}: {}",
                    integration.getCallbackUrl(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("تم قطع إرسال Outbound Webhook إلى {}", integration.getCallbackUrl());
        }
    }

    /**
     * بناء الـ JSON Payload لتحديث الحالة
     */
    private String buildStatusUpdatePayload(Order order, OrderStatus previousStatus, OrderStatus newStatus) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"event\":\"order.status.updated\",");
        json.append("\"orderCode\":\"").append(escapeJson(order.getCode())).append("\",");
        json.append("\"externalOrderId\":\"").append(escapeJson(order.getExternalOrderId())).append("\",");
        json.append("\"previousStatus\":\"").append(previousStatus.name()).append("\",");
        json.append("\"previousStatusArabic\":\"").append(escapeJson(previousStatus.getArabicName())).append("\",");
        json.append("\"newStatus\":\"").append(newStatus.name()).append("\",");
        json.append("\"newStatusArabic\":\"").append(escapeJson(newStatus.getArabicName())).append("\",");

        // معلومات إضافية
        if (order.getAmount() != null) {
            json.append("\"amount\":").append(order.getAmount()).append(",");
        }
        if (order.getCollectedAmount() != null) {
            json.append("\"collectedAmount\":").append(order.getCollectedAmount()).append(",");
        }
        if (order.getRejectionPayment() != null) {
            json.append("\"rejectionPayment\":").append(order.getRejectionPayment()).append(",");
        }
        if (order.getPartialDeliveryAmount() != null) {
            json.append("\"partialDeliveryAmount\":").append(order.getPartialDeliveryAmount()).append(",");
        }
        if (order.getDeliveredPieces() != null) {
            json.append("\"deliveredPieces\":").append(order.getDeliveredPieces()).append(",");
        }

        json.append("\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
        json.append("}");
        return json.toString();
    }

    /**
     * هروب الأحرف الخاصة في JSON
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
