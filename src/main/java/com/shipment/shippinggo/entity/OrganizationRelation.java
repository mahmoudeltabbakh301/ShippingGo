package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.RelationStatus;
import com.shipment.shippinggo.enums.RelationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "organization_relations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_organization_id", nullable = false)
    private Organization parentOrganization; // Company or requesting Office

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_organization_id", nullable = false)
    private Organization childOrganization; // Office being requested

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationStatus status = RelationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RelationType relationType = RelationType.OFFICE_TO_COMPANY;

    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
