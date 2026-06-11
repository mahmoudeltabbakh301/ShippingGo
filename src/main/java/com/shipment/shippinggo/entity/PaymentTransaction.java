package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // رقم العملية في Paymob
    @Column(name = "paymob_order_id")
    private String paymobOrderId;

    // رقم المعاملة في Paymob
    @Column(name = "paymob_transaction_id")
    private String paymobTransactionId;

    // المبلغ المدفوع
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    // العملة
    @Builder.Default
    @Column(length = 10)
    private String currency = "EGP";

    // حالة الدفع
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // طريقة الدفع (card / wallet / etc.)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    // التوقيع الأمني HMAC
    private String hmac;

    // الرد الكامل من Paymob
    @Column(name = "raw_response", length = 65535)
    private String rawResponse;

    // وقت الدفع الفعلي
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
