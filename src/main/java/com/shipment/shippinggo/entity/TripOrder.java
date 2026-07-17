package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "trip_orders", indexes = {
        @Index(name = "idx_trip_order_trip", columnList = "trip_id"),
        @Index(name = "idx_trip_order_order", columnList = "order_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_trip_order", columnNames = {"trip_id", "order_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // الرحلة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "tripOrders"})
    private Trip trip;

    // الأوردر
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order order;

    // وقت التحميل على الشاحنة
    @Column(nullable = false)
    private LocalDateTime loadedAt;

    // وقت التفريغ من الشاحنة
    private LocalDateTime unloadedAt;

    // هل هذا أوردر مرتجع (في رحلة العودة)
    @Builder.Default
    @Column(nullable = false)
    private boolean isReturn = false;

    @PrePersist
    protected void onCreate() {
        if (loadedAt == null) {
            loadedAt = LocalDateTime.now();
        }
    }
}
