package com.shipment.shippinggo.entity;

import com.shipment.shippinggo.enums.ShipmentRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipment_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(nullable = false)
    private String senderName;
    @Column(nullable = false)
    private String senderPhone;

    @Column(nullable = false)
    private String recipientName;
    @Column(nullable = false)
    private String recipientPhone;
    @Column(nullable = false)
    private String recipientAddress;

    private String contentDescription;
    private BigDecimal estimatedAmount;

    private BigDecimal shippingPrice;
    private BigDecimal orderPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentRequestStatus status;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ShipmentRequestStatus.PENDING;
        }
    }
}
