package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.PaymentTransaction;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.enums.PaymentStatus;
import com.shipment.shippinggo.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymobService {

    private final PlatformSettingService platformSettingService;
    private final SubscriptionService subscriptionService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ObjectMapper objectMapper;

    private static final String PAYMOB_INTENTION_URL = "https://accept.paymob.com/v1/intention/";

    public PaymobService(PlatformSettingService platformSettingService,
                          SubscriptionService subscriptionService,
                          PaymentTransactionRepository paymentTransactionRepository,
                          ObjectMapper objectMapper) {
        this.platformSettingService = platformSettingService;
        this.subscriptionService = subscriptionService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * إنشاء نية دفع (Payment Intention) على Paymob
     * @return client_secret للتوجيه لصفحة الدفع، أو null عند الفشل
     */
    public PaymentIntentionResult createPaymentIntention(Organization org, Subscription subscription) throws Exception {
        String secretKey = platformSettingService.getPaymobSecretKey();
        String integrationId = platformSettingService.getPaymobIntegrationId();

        if (secretKey == null || secretKey.isEmpty() || integrationId == null || integrationId.isEmpty()) {
            throw new IllegalStateException("إعدادات Paymob غير مكتملة. يرجى التواصل مع إدارة المنصة.");
        }

        BigDecimal amount = subscription.getMonthlyPrice();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("لم يتم تحديد سعر الاشتراك لهذه المنظمة بعد.");
        }

        // تحويل المبلغ للقروش (Paymob يتعامل بالقرش)
        int amountInCents = amount.multiply(new BigDecimal("100")).intValue();

        // بناء الطلب
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("amount", amountInCents);
        requestBody.put("currency", platformSettingService.getPlatformCurrency());

        // Integration IDs
        requestBody.put("payment_methods", List.of(Integer.parseInt(integrationId)));

        // Items
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", "اشتراك ShippingGo الشهري - " + org.getName());
        item.put("amount", amountInCents);
        item.put("quantity", 1);
        requestBody.put("items", List.of(item));

        // Billing Data
        Map<String, Object> billingData = new LinkedHashMap<>();
        billingData.put("first_name", org.getAdmin() != null ? org.getAdmin().getFullName() : org.getName());
        billingData.put("last_name", ".");
        billingData.put("email", org.getEmail() != null ? org.getEmail() : "noemail@shippinggo.com");
        billingData.put("phone_number", org.getPhone() != null ? org.getPhone() : "01000000000");
        requestBody.put("billing_data", billingData);

        // Extras (لربط المعاملة بالمنظمة)
        Map<String, String> extras = new LinkedHashMap<>();
        extras.put("organization_id", org.getId().toString());
        extras.put("subscription_id", subscription.getId().toString());
        requestBody.put("extras", extras);

        // Notification URL (Webhook)
        requestBody.put("notification_url", "https://shipping-go.com/api/webhooks/paymob");
        requestBody.put("redirection_url", "https://shipping-go.com/payment/callback");

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // إرسال الطلب لـ Paymob
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PAYMOB_INTENTION_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Token " + secretKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Paymob API Error: " + response.body());
        }

        // Parse response
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        String clientSecret = (String) responseMap.get("client_secret");
        String paymentKey = (String) responseMap.get("payment_key");
        Object intentionId = responseMap.get("id");

        // إنشاء سجل الدفع
        PaymentTransaction transaction = PaymentTransaction.builder()
                .subscription(subscription)
                .organization(org)
                .paymobOrderId(intentionId != null ? intentionId.toString() : null)
                .amount(amount)
                .currency(platformSettingService.getPlatformCurrency())
                .status(PaymentStatus.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        return new PaymentIntentionResult(clientSecret, paymentKey, transaction.getId());
    }

    /**
     * معالجة Webhook من Paymob
     */
    @Transactional
    public boolean handleWebhook(Map<String, Object> payload, String receivedHmac) {
        // 1. التحقق من HMAC
        if (!verifyHmac(payload, receivedHmac)) {
            System.err.println("Paymob Webhook: Invalid HMAC!");
            return false;
        }

        // 2. استخراج البيانات
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) payload.get("obj");
        if (obj == null) return false;

        boolean success = Boolean.TRUE.equals(obj.get("success"));
        Object transactionId = obj.get("id");
        Object orderId = obj.get("order");

        @SuppressWarnings("unchecked")
        Map<String, Object> orderObj = (orderId instanceof Map) ? (Map<String, Object>) orderId : null;
        String paymobOrderId = orderObj != null ? String.valueOf(orderObj.get("id")) : null;

        // 3. البحث عن المعاملة
        PaymentTransaction transaction = null;
        if (paymobOrderId != null) {
            transaction = paymentTransactionRepository.findByPaymobOrderId(paymobOrderId).orElse(null);
        }

        if (transaction == null) {
            System.err.println("Paymob Webhook: Transaction not found for order " + paymobOrderId);
            return false;
        }

        // 4. تحديث المعاملة
        transaction.setPaymobTransactionId(transactionId != null ? transactionId.toString() : null);
        transaction.setRawResponse(payload.toString());
        transaction.setHmac(receivedHmac);

        if (success) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setPaidAt(LocalDateTime.now());

            Object paymentMethodType = obj.get("source_data.type");
            if (paymentMethodType != null) {
                transaction.setPaymentMethod(paymentMethodType.toString());
            }

            // 5. تفعيل الاشتراك
            Long orgId = transaction.getOrganization().getId();
            subscriptionService.activateSubscription(orgId, 1); // شهر واحد
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
        }

        paymentTransactionRepository.save(transaction);
        return success;
    }

    /**
     * معالجة Callback (Redirect) بعد الدفع
     */
    @Transactional
    public PaymentTransaction handleCallback(Map<String, String> params) {
        String transactionId = params.get("id");
        String orderId = params.get("order");
        String successParam = params.get("success");
        String hmac = params.get("hmac");

        boolean success = "true".equalsIgnoreCase(successParam);

        // البحث عن المعاملة
        PaymentTransaction transaction = null;
        if (orderId != null) {
            transaction = paymentTransactionRepository.findByPaymobOrderId(orderId).orElse(null);
        }

        if (transaction == null) {
            return null;
        }

        // تحديث فقط إذا كانت PENDING (لأن الـ Webhook قد يكون سبقنا)
        if (transaction.getStatus() == PaymentStatus.PENDING) {
            transaction.setPaymobTransactionId(transactionId);
            transaction.setHmac(hmac);

            if (success) {
                transaction.setStatus(PaymentStatus.SUCCESS);
                transaction.setPaidAt(LocalDateTime.now());

                Long orgId = transaction.getOrganization().getId();
                subscriptionService.activateSubscription(orgId, 1);
            } else {
                transaction.setStatus(PaymentStatus.FAILED);
            }

            paymentTransactionRepository.save(transaction);
        }

        return transaction;
    }

    /**
     * التحقق من HMAC
     */
    public boolean verifyHmac(Map<String, Object> payload, String receivedHmac) {
        String hmacSecret = platformSettingService.getPaymobHmacSecret();
        if (hmacSecret == null || hmacSecret.isEmpty()) {
            // إذا لم يتم تعيين HMAC secret، نقبل (للتطوير فقط)
            System.out.println("Warning: HMAC secret not configured. Skipping verification.");
            return true;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) payload.get("obj");
            if (obj == null) return false;

            // ترتيب الحقول حسب توثيق Paymob
            StringBuilder data = new StringBuilder();
            String[] fields = {
                    "amount_cents", "created_at", "currency", "error_occured",
                    "has_parent_transaction", "id", "integration_id", "is_3d_secure",
                    "is_auth", "is_capture", "is_refunded", "is_standalone_payment",
                    "is_voided", "order.id", "owner", "pending", "source_data.pan",
                    "source_data.sub_type", "source_data.type", "success"
            };

            for (String field : fields) {
                Object value = getNestedValue(obj, field);
                if (value != null) {
                    data.append(value);
                }
            }

            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));

            // تحويل لـ hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(receivedHmac);
        } catch (Exception e) {
            System.err.println("HMAC verification error: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> map, String key) {
        String[] parts = key.split("\\.");
        Object current = map;
        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * الحصول على سجل المدفوعات لمنظمة
     */
    public List<PaymentTransaction> getPaymentsByOrganization(Long orgId) {
        return paymentTransactionRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    /**
     * إجمالي الإيرادات
     */
    public BigDecimal getTotalRevenue() {
        return paymentTransactionRepository.getTotalRevenue();
    }

    /**
     * إيرادات هذا الشهر
     */
    public BigDecimal getMonthlyRevenue() {
        return paymentTransactionRepository.getMonthlyRevenue();
    }

    // ===== Result DTOs =====

    public static class PaymentIntentionResult {
        private final String clientSecret;
        private final String paymentKey;
        private final Long transactionId;

        public PaymentIntentionResult(String clientSecret, String paymentKey, Long transactionId) {
            this.clientSecret = clientSecret;
            this.paymentKey = paymentKey;
            this.transactionId = transactionId;
        }

        public String getClientSecret() { return clientSecret; }
        public String getPaymentKey() { return paymentKey; }
        public Long getTransactionId() { return transactionId; }
    }
}
