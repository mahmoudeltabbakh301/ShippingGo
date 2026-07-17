package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips", indexes = {
        @Index(name = "idx_trip_origin_org", columnList = "origin_organization_id"),
        @Index(name = "idx_trip_dest_org", columnList = "destination_organization_id"),
        @Index(name = "idx_trip_vehicle", columnList = "vehicle_id"),
        @Index(name = "idx_trip_status", columnList = "status"),
        @Index(name = "idx_trip_code", columnList = "code"),
        @Index(name = "idx_trip_business_day", columnList = "business_day_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // كود الرحلة الفريد (يُولد تلقائياً — مثال: TR-5-1740000000-A3F2)
    @Column(nullable = false, unique = true)
    private String code;

    // الشاحنة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Vehicle vehicle;

    // المنظمة المرسلة (الأصل)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization originOrganization;

    // المنظمة المستلمة (الوجهة) — nullable للرحلات المحلية
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_organization_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization destinationOrganization;

    // يوم العمل
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_day_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private BusinessDay businessDay;

    // حالة الرحلة
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TripStatus status = TripStatus.PREPARING;

    // تاريخ الرحلة
    @Column(nullable = false)
    private LocalDate tripDate;

    // وقت المغادرة
    private LocalDateTime departedAt;

    // وقت الوصول
    private LocalDateTime arrivedAt;

    // وقت الاكتمال
    private LocalDateTime completedAt;

    // وقت بدء الرجوع
    private LocalDateTime returningAt;

    // وقت الوصول من الرجوع
    private LocalDateTime returnedAt;

    // المستخدم الذي أنشأ الرحلة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User createdBy;

    // ملاحظات
    @Column(length = 65535)
    private String notes;

    // عدد الطلبات في هذه الرحلة (denormalized for performance)
    @Builder.Default
    @Column(nullable = false)
    private int orderCount = 0;

    // الأوردرات المرتبطة بالرحلة
    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "trip"})
    @Builder.Default
    private List<TripOrder> tripOrders = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (tripDate == null) {
            tripDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
