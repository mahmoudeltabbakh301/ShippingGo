package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "organizations")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String address;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private User admin;

    @Column(columnDefinition = "TEXT")
    private String about;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private Governorate governorate;

    private boolean acceptsInternalShipments = true; // Default true
    private boolean acceptsExternalShipments = false; // Default false

    @Column(name = "is_virtual", nullable = false, columnDefinition = "boolean default false")
    private boolean isVirtual = false;

    @Column(name = "active", nullable = false, columnDefinition = "boolean default true")
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
