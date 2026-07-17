package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.OrderEventType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "order_events", indexes = {
        @Index(name = "idx_order_event_order", columnList = "order_id"),
        @Index(name = "idx_order_event_date", columnList = "action_date"),
        @Index(name = "idx_order_event_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "ownerOrganization", "creatorOrganization", "assignedToOrganization", "custodySetterOrganization", "createdBy", "assignedToCourier", "businessDay", "invoices"})
    private Order order;

    // نوع الحدث (تغيير حالة، إسناد، استلام مخزن، إلخ)
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    @Builder.Default
    private OrderEventType eventType = OrderEventType.STATUS_CHANGE;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private OrderStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "primaryOrganization", "password", "authorities", "verificationToken", "fcmToken"})
    private User user;

    // المنظمة المتعلقة بالحدث (مثل المنظمة المُسند إليها أو المؤكدة للاستلام)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "admin", "offices", "about", "pickupPolicy", "returnPolicy", "paymentTerms"})
    private Organization organization;

    @Column(length = 65535)
    private String notes;

    // بيانات إضافية مهيكلة (JSON) للأحداث المعقدة
    @Column(name = "metadata", length = 65535)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime actionDate;

    @PrePersist
    protected void onCreate() {
        actionDate = LocalDateTime.now();
    }
}
