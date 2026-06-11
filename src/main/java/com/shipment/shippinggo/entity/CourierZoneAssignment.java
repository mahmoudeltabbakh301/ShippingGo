package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.Governorate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ربط مندوب بمنطقة جغرافية — المندوب يمكن أن يكون مسؤولاً عن أكتر من منطقة.
 * يُستخدم في التوزيع التلقائي للأوردرات على المناديب.
 *
 * الهيكل: محافظة → مركز (اختياري) → منطقة (اختياري)
 */
@Entity
@Table(name = "courier_zone_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"courier_id", "organization_id", "governorate", "district", "center", "area"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierZoneAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private User courier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Governorate governorate;

    // المنطقة/المركز القديم (محتفظ به للتوافق)
    @Column(name = "district")
    private String district;

    // المركز (اختياري — إذا null يعني المندوب مسؤول عن كل المحافظة)
    @Column(name = "center")
    private String center;

    // المنطقة داخل المركز (اختياري — إذا null يعني المندوب مسؤول عن كل المركز)
    @Column(name = "area")
    private String area;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
