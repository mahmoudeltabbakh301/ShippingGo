package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.SupportTicket;
import com.shipment.shippinggo.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Page<SupportTicket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    long countByStatus(TicketStatus status);

    long countByOrganizationId(Long organizationId);
}
