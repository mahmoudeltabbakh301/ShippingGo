package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_notifications", indexes = {
        @Index(name = "idx_notif_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notif_user_read", columnList = "user_id, is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "primaryOrganization", "password", "authorities", "verificationToken", "fcmToken"})
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "`type`", nullable = false)
    private String type;

    // الرابط الذي ينتقل إليه المستخدم عند النقر على الإشعار
    @Column(length = 500)
    private String linkUrl;

    // معرف الكائن المرجعي (مثل orderId) لربط الإشعار بكائن محدد
    private Long referenceId;

    @Column(nullable = false)
    private boolean isRead = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
