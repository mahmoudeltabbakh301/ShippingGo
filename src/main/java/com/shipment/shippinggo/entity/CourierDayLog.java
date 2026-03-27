package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "courier_day_logs", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "courier_id", "date" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierDayLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "courier_id", nullable = false)
    private User courier;

    @Column(nullable = false)
    private LocalDate date;

    // إحصائيات اليوم
    @Builder.Default
    private int totalOrders = 0;
    @Builder.Default
    private int deliveredCount = 0;
    @Builder.Default
    private int refusedCount = 0;
    @Builder.Default
    private int cancelledCount = 0;
    @Builder.Default
    private int inTransitCount = 0;

    @Builder.Default
    private java.math.BigDecimal deliveredAmount = java.math.BigDecimal.ZERO;

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
