package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.CommissionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.shipment.shippinggo.enums.Governorate;

@Entity
@Table(name = "commission_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // المنظمة المصدر (الشركة أو المكتب الذي يدفع العمولة)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_organization_id", nullable = false)
    private Organization sourceOrganization;

    // المنظمة المستلمة (المكتب الذي يستلم العمولة) - nullable للمناديب
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id")
    private Organization targetOrganization;

    // المندوب (في حالة عمولة المندوب) - nullable للعلاقات بين المنظمات
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    // المحافظة (في حالة عمولة المنظمة) - nullable للعمولة العامة (الافتراضية)
    @Enumerated(EnumType.STRING)
    @Column(name = "governorate")
    private Governorate governorate;

    // نوع العمولة (نسبة مئوية أو مبلغ ثابت)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommissionType commissionType = CommissionType.FIXED;

    // قيمة العمولة
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal commissionValue;

    // عمولة الرفض (مبلغ ثابت يُخصم عند الرفض)
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal rejectionCommission = BigDecimal.ZERO;

    // عمولة الإلغاء (مبلغ ثابت يُخصم عند الإلغاء)
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cancellationCommission = BigDecimal.ZERO;

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
}
