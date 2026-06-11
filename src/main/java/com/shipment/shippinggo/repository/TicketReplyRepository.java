package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.TicketReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketReplyRepository extends JpaRepository<TicketReply, Long> {

    List<TicketReply> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
