package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.Governorate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * منطقة مخصصة تضيفها المنظمة — لتوسيع قائمة المناطق الأساسية الموجودة في JSON.
 * يمكن للمنظمات إضافة مناطق/قرى/شوارع خاصة بها لكل مركز.
 */
@Entity
@Table(name = "custom_areas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "governorate", "center", "name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Governorate governorate;

    /** المركز الذي تتبعه المنطقة */
    @Column(nullable = false)
    private String center;

    /** اسم المنطقة بالعربي */
    @Column(nullable = false)
    private String name;

    /** اسم المنطقة بالإنجليزي (اختياري) */
    @Column(name = "name_en")
    private String nameEn;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
