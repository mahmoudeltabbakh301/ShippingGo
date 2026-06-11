package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.CommissionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "target_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TargetSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // المنظمة المصدر (الشركة أو المكتب الذي يحدد التارجت)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_organization_id", nullable = false)
    private Organization sourceOrganization;

    // المنظمة المستهدفة (المكتب الذي عليه التارجت) - nullable للمناديب
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id")
    private Organization targetOrganization;

    // المندوب (في حالة تارجت المندوب) - nullable للعلاقات بين المنظمات
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    // قيمة التارجت (المبلغ المطلوب تحقيقه - مثلاً 10000)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    // نوع المكافأة (نسبة مئوية أو مبلغ ثابت)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommissionType rewardType = CommissionType.FIXED;

    // قيمة المكافأة (مبلغ ثابت أو نسبة مئوية)
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal rewardValue;

    // تاريخ بداية احتساب التارجت (يبدأ العد من هذا التاريخ شهرياً)
    @Column(nullable = false)
    private LocalDateTime startDate;

    // هل التارجت نشط
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) {
            startDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
