package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.OrderEvent;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderEventType;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository orderEventRepository;

    // تسجيل حدث تغيّر حالة الطلب في سجل الأحداث (لأغراض المتابعة والمراجعة)
    @SuppressWarnings("null")
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordEvent(Order order, OrderStatus previousStatus, OrderStatus newStatus, User user, String notes) {
        OrderEvent event = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.STATUS_CHANGE)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .user(user)
                .notes(notes)
                .build();

        orderEventRepository.save(event);
    }

    /**
     * تسجيل حدث مصنّف (typed event) — يدعم جميع أنواع الأحداث بما فيها
     * الإسناد، قبول الإسناد، استلام المخزن، إلخ.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordTypedEvent(Order order, OrderEventType type, User user,
                                 Organization org, String notes) {
        OrderEvent event = OrderEvent.builder()
                .order(order)
                .eventType(type)
                .user(user)
                .organization(org)
                .notes(notes)
                .build();

        orderEventRepository.save(event);
    }

    /**
     * تسجيل حدث مصنّف مع بيانات إضافية (metadata).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void recordTypedEvent(Order order, OrderEventType type, User user,
                                 Organization org, String notes, String metadata) {
        OrderEvent event = OrderEvent.builder()
                .order(order)
                .eventType(type)
                .user(user)
                .organization(org)
                .notes(notes)
                .metadata(metadata)
                .build();

        orderEventRepository.save(event);
    }

    /**
     * التحقق من وقوع حدث معين لطلب محدد.
     */
    public boolean hasEvent(Long orderId, OrderEventType type) {
        return orderEventRepository.existsByOrderIdAndEventType(orderId, type);
    }

    /**
     * التحقق من وقوع حدث معين لطلب محدد من منظمة معينة.
     */
    public boolean hasEventForOrganization(Long orderId, OrderEventType type, Long organizationId) {
        return orderEventRepository.existsByOrderIdAndEventTypeAndOrganizationId(orderId, type, organizationId);
    }

    // استرجاع سجل الأحداث الخاص بطلب معين
    public List<OrderEvent> getOrderEvents(Long orderId) {
        return orderEventRepository.findByOrderIdOrderByActionDateDesc(orderId);
    }

    // استرجاع أحداث من نوع معين لطلب معين
    public List<OrderEvent> getOrderEventsByType(Long orderId, OrderEventType type) {
        return orderEventRepository.findByOrderIdAndEventTypeOrderByActionDateDesc(orderId, type);
    }
}
