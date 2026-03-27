package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.OrderEvent;
import com.shipment.shippinggo.entity.User;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(Order order, OrderStatus previousStatus, OrderStatus newStatus, User user, String notes) {
        OrderEvent event = OrderEvent.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .user(user)
                .notes(notes)
                .build();

        orderEventRepository.save(event);
    }

    // استرجاع سجل الأحداث الخاص بطلب معين
    public List<OrderEvent> getOrderEvents(Long orderId) {
        return orderEventRepository.findByOrderIdOrderByActionDateDesc(orderId);
    }
}
