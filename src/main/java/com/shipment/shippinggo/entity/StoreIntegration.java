package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.IntegrationPlatform;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_integrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "store_id", "platform" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreIntegration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    // المنظمة المرتبطة (شركة أو مكتب) - بديل عن store للأنظمة الخارجية
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationPlatform platform;

    // مفتاح فريد (UUID) للتحقق من صحة الـ Webhook
    @Column(name = "webhook_api_key", nullable = false, unique = true)
    private String webhookApiKey;

    // رابط المتجر الخارجي (اختياري - للعرض فقط)
    @Column(name = "external_store_url")
    private String externalStoreUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // رابط الـ Callback لإرسال تحديثات الحالة للنظام الخارجي (Outbound Webhook)
    @Column(name = "callback_url")
    private String callbackUrl;

    // مفتاح سري للتحقق من هوية ShippingGo عند إرسال التحديثات
    @Column(name = "callback_secret")
    private String callbackSecret;

    // آخر مرة وصل فيها طلب من هذا الربط
    @Column(name = "last_webhook_received_at")
    private LocalDateTime lastWebhookReceivedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
