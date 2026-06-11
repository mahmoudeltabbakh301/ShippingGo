package com.shipment.shippinggo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_replies", indexes = {
        @Index(name = "idx_reply_ticket", columnList = "ticket_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(nullable = false, length = 65535)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_id", nullable = false)
    private User repliedBy;

    @Column(nullable = false)
    private boolean adminReply;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
