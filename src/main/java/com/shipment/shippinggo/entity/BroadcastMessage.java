package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.BroadcastTargetType;
import com.shipment.shippinggo.enums.BroadcastType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "broadcast_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 65535)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "`type`", nullable = false)
    private BroadcastType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BroadcastTargetType targetType;

    // إذا كان الهدف منظمة محددة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id")
    private Organization targetOrganization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_by_id", nullable = false)
    private User sentBy;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
