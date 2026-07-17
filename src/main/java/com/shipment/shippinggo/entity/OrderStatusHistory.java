package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private OrderStatus newStatus;

    // السعر السابق
    @Column(precision = 10, scale = 2)
    private BigDecimal previousAmount;

    // السعر الجديد
    @Column(precision = 10, scale = 2)
    private BigDecimal newAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private User changedBy;

    @Column(length = 65535)
    private String notes;

    // سبب الرفض/الإلغاء/التأجيل
    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_reason")
    private com.shipment.shippinggo.enums.RejectionReason rejectionReason;

    // تفاصيل سبب الرفض
    @Column(name = "rejection_reason_notes", length = 65535)
    private String rejectionReasonNotes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = LocalDateTime.now();
    }
}
