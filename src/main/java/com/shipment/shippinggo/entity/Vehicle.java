package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.VehicleStatus;
import com.shipment.shippinggo.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles", indexes = {
        @Index(name = "idx_vehicle_org", columnList = "organization_id"),
        @Index(name = "idx_vehicle_code", columnList = "code"),
        @Index(name = "idx_vehicle_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // كود الشاحنة الفريد (يُولد تلقائياً — مثال: VH-5-1740000000-A3F2)
    @Column(nullable = false, unique = true)
    private String code;

    // رقم اللوحة
    @Column(nullable = false)
    private String plateNumber;

    // نوع الشاحنة
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    // الموديل
    private String model;

    // اللون
    private String color;

    // السعة (عدد الطلبات أو الكراتين)
    private Integer capacity;

    // اسم السائق (نصي — قابل للتعديل بحرية)
    private String driverName;

    // رقم هاتف السائق
    private String driverPhone;

    // حالة الشاحنة
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    // المنظمة المالكة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Organization organization;

    // ملاحظات
    @Column(length = 65535)
    private String notes;

    // هل الشاحنة فعالة
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

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
