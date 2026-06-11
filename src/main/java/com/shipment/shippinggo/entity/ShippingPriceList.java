package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * قائمة أسعار الشحن — كل منظمة يمكنها إنشاء قوائم متعددة (عادي / مستعجل / إلخ).
 * الأسعار للعملاء الخارجيين فقط وليس للمنظمات المشتركة.
 */
@Entity
@Table(name = "shipping_price_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingPriceList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name; // "أسعار عادية" / "أسعار مستعجلة"

    @Builder.Default
    @Column(nullable = false)
    private boolean isDefault = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "priceList", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShippingPriceEntry> entries = new ArrayList<>();

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
