package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.*;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderStatusService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderEventService orderEventService;
    private final AccountService accountService;
    private final OrderAssignmentService orderAssignmentService;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderStatusService(OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            OrderEventService orderEventService,
            AccountService accountService,
            OrderAssignmentService orderAssignmentService,
            NotificationService notificationService,
            SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderEventService = orderEventService;
        this.accountService = accountService;
        this.orderAssignmentService = orderAssignmentService;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Order updateStatus(Long orderId, OrderStatus newStatus, BigDecimal newAmount, User changedBy, String notes) {
        return updateStatusAdvanced(orderId, newStatus, newAmount, null, null, null, changedBy, notes);
    }

    @Transactional
    public Order updateStatusWithRejectionPayment(Long orderId, OrderStatus newStatus, BigDecimal newAmount,
            BigDecimal rejectionPayment, User changedBy, String notes) {
        return updateStatusAdvanced(orderId, newStatus, newAmount, rejectionPayment, null, null, changedBy, notes);
    }

    // تحديث حالة الطلب بشكل متقدم وتطبيق قواعد وشروط الانتقال والتأكد من الصلاحيات
    @Transactional
    public Order updateStatusAdvanced(Long orderId, OrderStatus newStatus, BigDecimal newAmount,
            BigDecimal rejectionPayment, Integer deliveredPieces, BigDecimal partialDeliveryAmount, User changedBy,
            String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.REFUSED
                || currentStatus == OrderStatus.CANCELLED) {
            if (newStatus == OrderStatus.IN_TRANSIT) {
                Organization assignedOrg = order.getAssignedToOrganization();
                if (assignedOrg == null || !orderAssignmentService.isUserMemberOfOrganization(changedBy, assignedOrg)) {
                    throw new UnauthorizedAccessException(
                            "فقط المكتب المسند إليه الطلب يمكنه إعادة الحالة إلى 'في الطريق' لتصحيح خطأ.");
                }
                order.setProcessedByCourier(false);
            } else {
                throw new BusinessLogicException("لا يمكن تغيير حالة طلب في حالة نهائية ("
                        + currentStatus.getArabicName() + "). يمكن فقط إعادة الحالة إلى 'في الطريق' لتصحيح خطأ.");
            }
        }

        if (order.isProcessedByCourier() && order.getAssignedToCourier() != null
                && changedBy.getId().equals(order.getAssignedToCourier().getId())) {
            throw new BusinessLogicException("المندوب قام بتحديث هذا الطلب مسبقاً");
        }

        Organization assignedOrg = order.getAssignedToOrganization();
        if (assignedOrg != null) {
            boolean isAssignedCourier = order.getAssignedToCourier() != null
                    && changedBy.getId().equals(order.getAssignedToCourier().getId());
            boolean isFromAssignedOrg = orderAssignmentService.isUserMemberOfOrganization(changedBy, assignedOrg);

            if (!isAssignedCourier && !isFromAssignedOrg) {
                throw new UnauthorizedAccessException(
                        "لا يمكنك تغيير حالة هذا الطلب. فقط المكتب المسند إليه الطلب أو المندوب المعين يمكنه ذلك.");
            }
        }

        OrderStatus previousStatus = order.getStatus();
        BigDecimal previousAmount = order.getAmount();

        order.setStatus(newStatus);

        if (order.getAssignedToCourier() != null && changedBy.getId().equals(order.getAssignedToCourier().getId())) {
            order.setProcessedByCourier(true);
        }

        if (newAmount != null) {
            boolean isCourier = changedBy.getRole() == com.shipment.shippinggo.enums.Role.COURIER;

            if (isCourier) {
                if (!newAmount.equals(previousAmount)) {
                    order.setCollectedAmount(newAmount);
                    BigDecimal safePreviousAmount = (previousAmount != null) ? previousAmount : BigDecimal.ZERO;
                    String systemNote = String.format("تم تحصيل مبلغ %.2f بدلاً من %.2f", newAmount,
                            safePreviousAmount);
                    notes = (notes == null || notes.isEmpty()) ? systemNote : notes + " - " + systemNote;
                }
            } else {
                if (!newAmount.equals(previousAmount)) {
                    order.setAmount(newAmount);
                }
            }
        }

        if (newStatus == OrderStatus.REFUSED && rejectionPayment != null) {
            order.setRejectionPayment(rejectionPayment);
        }

        if (newStatus == OrderStatus.PARTIAL_DELIVERY) {
            if (deliveredPieces != null && deliveredPieces > 0) {
                if (order.getQuantity() != null && order.getQuantity() >= deliveredPieces) {
                    order.setQuantity(order.getQuantity() - deliveredPieces);
                }
                order.setDeliveredPieces(deliveredPieces);
            }
            if (partialDeliveryAmount != null) {
                order.setPartialDeliveryAmount(partialDeliveryAmount);
            }
        }

        recordStatusChange(order, previousStatus, newStatus, changedBy, previousAmount, newAmount, notes);

        Order savedOrder = orderRepository.save(order);

        if (newStatus == OrderStatus.DELIVERED) {
            accountService.processOrderDeliveryCommissions(savedOrder);
        } else if (newStatus == OrderStatus.REFUSED) {
            accountService.processOrderRejectionCommissions(savedOrder);
        } else if (newStatus == OrderStatus.CANCELLED) {
            accountService.processOrderCancellationCommissions(savedOrder);
        } else if (newStatus == OrderStatus.PARTIAL_DELIVERY) {
            accountService.processOrderDeliveryCommissions(savedOrder);
        } else {
            // معالجة عمولة المندوب الفردية لباقي الحالات (مثل مؤجل)
            accountService.processManualCourierCommission(savedOrder);
        }

        return savedOrder;
    }

    // تسجيل تاريخ تحديث حالة الطلب كعملية منفصلة في اللوج الخاص بالطلب
    public void recordStatusChange(Order order, OrderStatus previousStatus, OrderStatus newStatus,
            User changedBy, BigDecimal previousAmount, BigDecimal newAmount, String notes) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .previousAmount(previousAmount)
                .newAmount(newAmount)
                .changedBy(changedBy)
                .notes(notes)
                .build();

        orderStatusHistoryRepository.save(history);
        orderEventService.recordEvent(order, previousStatus, newStatus, changedBy, notes);

        // Broadcast order status update via STOMP WebSocket
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("orderId", order.getId());
            payload.put("orderCode", order.getCode());
            payload.put("status", newStatus.name());
            payload.put("statusArabic", newStatus.getArabicName());
            payload.put("timestamp", java.time.Instant.now().toString());

            messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), payload);
            if (order.getOwnerOrganization() != null) {
                messagingTemplate.convertAndSend("/topic/orgs/" + order.getOwnerOrganization().getId() + "/orders",
                        payload);
            }

            // Send Push Notification to Owner Organization
            if (order.getOwnerOrganization() != null) {
                notificationService.sendOrderStatusUpdateNotification(
                    order.getOwnerOrganization(), 
                    order.getCode(), 
                    newStatus.getArabicName()
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to broadcast WebSocket or Push message: " + e.getMessage());
        }
    }

    // تأكيد استلام المرتجع والتأكد من سلسلة الإسناد للسماح باستكمال عملية الإرجاع
    @Transactional
    public Order confirmReturn(Long orderId, User confirmedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (order.getStatus() != OrderStatus.REFUSED && order.getStatus() != OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.PARTIAL_DELIVERY
                && order.getStatus() != OrderStatus.DEFERRED) {
            throw new BusinessLogicException(
                    "لا يمكن تأكيد استلام المرتجع إلا للطلبات المرفوضة، الملغية، المؤجلة، أو ذات الاستلام الجزئي.");
        }

        if (order.isReturnedToOwner()) {
            throw new BusinessLogicException("تم إرجاع هذا الطلب بالكامل للمالك الأصلي.");
        }

        Organization userOrg = orderAssignmentService.resolveUserOrganization(confirmedBy);
        if (userOrg == null) {
            throw new UnauthorizedAccessException("لم يتم العثور على منظمة المستخدم.");
        }

        List<OrderAssignment> chain = orderAssignmentRepository.findByOrderIdOrderByLevelAsc(orderId);

        if (chain.isEmpty()) {
            if (userOrg.getId().equals(order.getOwnerOrganization().getId())) {
                order.setReturnedToOwner(true);
                return orderRepository.save(order);
            }
            throw new BusinessLogicException("لا يوجد سلسلة إسناد لهذا الطلب.");
        }

        OrderAssignment pendingAssignment = null;
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (!chain.get(i).isReturnConfirmed()) {
                pendingAssignment = chain.get(i);
                break;
            }
        }

        if (pendingAssignment == null) {
            if (userOrg.getId().equals(order.getOwnerOrganization().getId())) {
                order.setReturnedToOwner(true);
                return orderRepository.save(order);
            }
            throw new BusinessLogicException("تم تأكيد جميع المراحل. بانتظار تأكيد المالك الأصلي.");
        }

        Organization rawAssigneeOrg = pendingAssignment.getAssigneeOrganization();
        Organization unproxiedAssigneeOrg = org.hibernate.Hibernate.unproxy(rawAssigneeOrg, Organization.class);

        boolean canConfirmOnBehalf = false;
        if (unproxiedAssigneeOrg instanceof VirtualOffice) {
            VirtualOffice vo = (VirtualOffice) unproxiedAssigneeOrg;
            if (vo.getParentOrganization() != null && vo.getParentOrganization().getId().equals(userOrg.getId())) {
                canConfirmOnBehalf = true;
            }
        }

        if (!userOrg.getId().equals(rawAssigneeOrg.getId()) && !canConfirmOnBehalf) {
            throw new UnauthorizedAccessException("ليس دورك لتأكيد استلام المرتجع. الدور الحالي للمنظمة: "
                    + rawAssigneeOrg.getName());
        }

        pendingAssignment.setReturnConfirmed(true);
        pendingAssignment.setReturnConfirmedAt(LocalDateTime.now());
        orderAssignmentRepository.save(pendingAssignment);

        // إذا تم التأكيد نيابة عن المكتب الافتراضي، وكان الدور القادم على المنظمة الأساسية،
        // نقوم بتأكيد استلام المنظمة الأساسية تلقائياً لأنها تسلمت الطلب بالفعل.
        if (canConfirmOnBehalf) {
            OrderAssignment nextPending = null;
            for (int i = chain.size() - 1; i >= 0; i--) {
                if (!chain.get(i).isReturnConfirmed()) {
                    nextPending = chain.get(i);
                    break;
                }
            }
            
            if (nextPending != null && nextPending.getAssigneeOrganization().getId().equals(userOrg.getId())) {
                nextPending.setReturnConfirmed(true);
                nextPending.setReturnConfirmedAt(LocalDateTime.now());
                orderAssignmentRepository.save(nextPending);
            } else if (nextPending == null && userOrg.getId().equals(order.getOwnerOrganization().getId())) {
                order.setReturnedToOwner(true);
            }
        }

        return orderRepository.save(order);
    }

    // معرفة حالة إرجاع الطلب بالتفصيل لكل المنظمات المتورطة في إرساله
    public java.util.Map<String, Object> getReturnStatus(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("returnedToOwner", order.isReturnedToOwner());

        List<OrderAssignment> chain = orderAssignmentRepository.findByOrderIdOrderByLevelAsc(orderId);
        status.put("totalLevels", chain.size());

        if (chain.isEmpty()) {
            status.put("allConfirmed", true);
            status.put("currentOrgName", order.getOwnerOrganization().getName());
            return status;
        }

        boolean allConfirmed = true;
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (!chain.get(i).isReturnConfirmed()) {
                allConfirmed = false;
                status.put("currentLevel", chain.get(i).getLevel());
                status.put("currentOrgName", chain.get(i).getAssigneeOrganization().getName());
                break;
            }
        }
        status.put("allConfirmed", allConfirmed);
        if (allConfirmed) {
            status.put("currentOrgName", order.getOwnerOrganization().getName());
        }

        List<java.util.Map<String, Object>> levels = new java.util.ArrayList<>();
        for (OrderAssignment oa : chain) {
            java.util.Map<String, Object> lvl = new java.util.HashMap<>();
            lvl.put("level", oa.getLevel());
            lvl.put("assigneeName", oa.getAssigneeOrganization().getName());
            lvl.put("assignerName", oa.getAssignerOrganization().getName());
            lvl.put("returnConfirmed", oa.isReturnConfirmed());
            levels.add(lvl);
        }
        status.put("chainLevels", levels);

        return status;
    }
}
