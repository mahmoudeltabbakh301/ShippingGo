package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // الأوردر المرتبط بالمعاملة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // المنظمة (الشركة أو المكتب)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    // اسم المنظمة (يُحفظ عند حذف المكتب الافتراضي)
    @Column(name = "organization_name")
    private String organizationName;

    // المندوب (في حالة عمولة المندوب)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id")
    private User courier;

    // مبلغ العمولة
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    // مبلغ الأوردر الأصلي
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal orderAmount;

    // الوصف
    @Column(length = 65535)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ربط بيوم الحسابات
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_business_day_id")
    private AccountBusinessDay accountBusinessDay;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
