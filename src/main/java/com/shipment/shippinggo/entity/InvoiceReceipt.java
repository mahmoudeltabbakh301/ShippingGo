package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_receipts", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "invoice_id", "organization_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // المنظمة التي يجب أن تؤكد الاستلام
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // ترتيب التأكيد في السلسة (1 = آخر طرف مسند إليه، الرقم الأكبر = المالك الأصلي)
    @Column(nullable = false)
    private int confirmationOrder;

    // هل تم التأكيد
    @Column(nullable = false)
    @Builder.Default
    private boolean confirmed = false;

    // وقت التأكيد
    private LocalDateTime confirmedAt;

    // المستخدم الذي أكد الاستلام
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private User confirmedBy;
}
