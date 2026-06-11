package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    // السعر الشهري المحدد من السوبر أدمن لهذه المنظمة
    @Column(precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    // بداية الفترة التجريبية
    @Column(nullable = false)
    private LocalDateTime trialStartDate;

    // نهاية الفترة التجريبية
    @Column(nullable = false)
    private LocalDateTime trialEndDate;

    // بداية فترة الاشتراك الحالية (بعد الدفع)
    private LocalDateTime currentPeriodStart;

    // نهاية فترة الاشتراك الحالية
    private LocalDateTime currentPeriodEnd;

    // تجديد تلقائي
    @Builder.Default
    private boolean autoRenew = true;

    // ملاحظات السوبر أدمن
    @Column(length = 65535)
    private String notes;

    // آخر إشعار تنبيهي تم إرساله (لمنع التكرار)
    private LocalDateTime lastNotificationSentAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * هل الاشتراك فعال (تجريبي ولم ينته أو نشط ولم ينته)؟
     */
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        if (status == SubscriptionStatus.TRIAL) {
            return trialEndDate != null && now.isBefore(trialEndDate);
        }
        if (status == SubscriptionStatus.ACTIVE) {
            return currentPeriodEnd != null && now.isBefore(currentPeriodEnd);
        }
        return false;
    }

    /**
     * عدد الأيام المتبقية
     */
    public long getRemainingDays() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = (status == SubscriptionStatus.TRIAL) ? trialEndDate : currentPeriodEnd;
        if (endDate == null || now.isAfter(endDate)) return 0;
        return java.time.Duration.between(now, endDate).toDays();
    }
}
