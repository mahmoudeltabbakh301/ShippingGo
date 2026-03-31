package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_inquiries", indexes = {
        @Index(name = "idx_oi_order", columnList = "order_id"),
        @Index(name = "idx_oi_receiver", columnList = "receiver_organization_id"),
        @Index(name = "idx_oi_sender", columnList = "sender_organization_id"),
        @Index(name = "idx_oi_receiver_bd", columnList = "receiver_organization_id, business_day_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_oi_order_receiver", columnNames = { "order_id", "receiver_organization_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // الأوردر المُستعلم عنه
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Order order;

    // المنظمة المُرسِلة (المالكة للأوردر)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_organization_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Organization senderOrganization;

    // المنظمة المُستلمة للاستعلام
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_organization_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Organization receiverOrganization;

    // يوم عمل المنظمة المستلمة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_day_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private BusinessDay businessDay;

    // المستخدم الذي أرسل الاستعلام
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private User sentBy;

    // ملاحظة اختيارية
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
