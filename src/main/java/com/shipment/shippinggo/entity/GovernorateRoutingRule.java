package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.Governorate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "governorate_routing_rules", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "store_id", "governorate" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GovernorateRoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Governorate governorate;

    // شركة الشحن المستهدفة لهذه المحافظة
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_organization_id", nullable = false)
    private Organization targetOrganization;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
