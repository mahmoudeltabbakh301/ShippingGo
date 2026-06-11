package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.TicketCategory;
import com.shipment.shippinggo.enums.TicketPriority;
import com.shipment.shippinggo.enums.TicketStatus;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.SupportTicketRepository;
import com.shipment.shippinggo.repository.TicketReplyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketReplyRepository replyRepository;
    private final NotificationService notificationService;

    public SupportTicketService(SupportTicketRepository ticketRepository,
                                 TicketReplyRepository replyRepository,
                                 NotificationService notificationService) {
        this.ticketRepository = ticketRepository;
        this.replyRepository = replyRepository;
        this.notificationService = notificationService;
    }

    // ===== Create =====

    @Transactional
    public SupportTicket createTicket(User user, Organization org, String subject, String description,
                                      TicketPriority priority, TicketCategory category) {
        SupportTicket ticket = SupportTicket.builder()
                .subject(subject)
                .description(description)
                .status(TicketStatus.OPEN)
                .priority(priority)
                .category(category)
                .organization(org)
                .createdBy(user)
                .build();

        return ticketRepository.save(ticket);
    }

    // ===== Reply =====

    @Transactional
    public TicketReply replyToTicket(Long ticketId, User user, String message, boolean isAdmin) {
        SupportTicket ticket = getTicketById(ticketId);

        TicketReply reply = TicketReply.builder()
                .ticket(ticket)
                .message(message)
                .repliedBy(user)
                .adminReply(isAdmin)
                .build();

        reply = replyRepository.save(reply);

        // تحديث حالة التذكرة تلقائياً
        if (isAdmin && ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        // إرسال إشعار
        if (isAdmin) {
            // إشعار لمنشئ التذكرة
            notificationService.sendNotificationToUser(ticket.getCreatedBy(),
                    "رد على تذكرة الدعم",
                    "تم الرد على تذكرتك: " + ticket.getSubject(),
                    java.util.Map.of("type", "SUPPORT_REPLY", "ticketId", ticketId.toString()),
                    "SUPPORT", "/support/" + ticketId, ticketId);
        }

        return reply;
    }

    // ===== Status =====

    @Transactional
    public SupportTicket updateTicketStatus(Long ticketId, TicketStatus newStatus) {
        SupportTicket ticket = getTicketById(ticketId);
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.CLOSED || newStatus == TicketStatus.RESOLVED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        ticket = ticketRepository.save(ticket);

        // إشعار لمنشئ التذكرة
        notificationService.sendNotificationToUser(ticket.getCreatedBy(),
                "تحديث حالة تذكرة الدعم",
                "تم تحديث حالة تذكرتك (" + ticket.getSubject() + ") إلى: " + newStatus.getArabicName(),
                java.util.Map.of("type", "SUPPORT_STATUS", "ticketId", ticketId.toString()),
                "SUPPORT", "/support/" + ticketId, ticketId);

        return ticket;
    }

    // ===== Query =====

    public SupportTicket getTicketById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("التذكرة غير موجودة"));
    }

    public List<SupportTicket> getTicketsByOrganization(Long orgId) {
        return ticketRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    public Page<SupportTicket> getAllTickets(TicketStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null) {
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<TicketReply> getTicketReplies(Long ticketId) {
        return replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    // ===== Stats =====

    public TicketStats getTicketStats() {
        long total = ticketRepository.count();
        long open = ticketRepository.countByStatus(TicketStatus.OPEN);
        long inProgress = ticketRepository.countByStatus(TicketStatus.IN_PROGRESS);
        long resolved = ticketRepository.countByStatus(TicketStatus.RESOLVED);
        long closed = ticketRepository.countByStatus(TicketStatus.CLOSED);

        return new TicketStats(total, open, inProgress, resolved, closed);
    }

    public static class TicketStats {
        private final long total;
        private final long open;
        private final long inProgress;
        private final long resolved;
        private final long closed;

        public TicketStats(long total, long open, long inProgress, long resolved, long closed) {
            this.total = total;
            this.open = open;
            this.inProgress = inProgress;
            this.resolved = resolved;
            this.closed = closed;
        }

        public long getTotal() { return total; }
        public long getOpen() { return open; }
        public long getInProgress() { return inProgress; }
        public long getResolved() { return resolved; }
        public long getClosed() { return closed; }
    }
}
