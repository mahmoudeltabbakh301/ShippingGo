package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.OrderEvent;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventServiceTest {

    @Mock
    private OrderEventRepository orderEventRepository;

    @InjectMocks
    private OrderEventService orderEventService;

    @Test
    void recordEvent_ShouldSaveEventWithCorrectData() {
        Order order = new Order();
        order.setId(1L);
        User user = new User();
        user.setId(1L);

        orderEventService.recordEvent(order, OrderStatus.WAITING, OrderStatus.IN_TRANSIT, user, "test note");

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(orderEventRepository).save(captor.capture());

        OrderEvent savedEvent = captor.getValue();
        assertEquals(order, savedEvent.getOrder());
        assertEquals(OrderStatus.WAITING, savedEvent.getPreviousStatus());
        assertEquals(OrderStatus.IN_TRANSIT, savedEvent.getNewStatus());
        assertEquals(user, savedEvent.getUser());
        assertEquals("test note", savedEvent.getNotes());
    }

    @Test
    void recordEvent_WithNullNotes_ShouldStillSave() {
        Order order = new Order();
        User user = new User();

        orderEventService.recordEvent(order, OrderStatus.WAITING, OrderStatus.IN_TRANSIT, user, null);

        verify(orderEventRepository).save(any(OrderEvent.class));
    }

    @Test
    void getOrderEvents_ShouldReturnEvents() {
        Long orderId = 1L;
        List<OrderEvent> expected = List.of(new OrderEvent(), new OrderEvent());
        when(orderEventRepository.findByOrderIdOrderByActionDateDesc(orderId)).thenReturn(expected);

        List<OrderEvent> result = orderEventService.getOrderEvents(orderId);

        assertEquals(expected, result);
        assertEquals(2, result.size());
    }

    @Test
    void getOrderEvents_WhenNoEvents_ShouldReturnEmptyList() {
        when(orderEventRepository.findByOrderIdOrderByActionDateDesc(99L)).thenReturn(List.of());

        List<OrderEvent> result = orderEventService.getOrderEvents(99L);

        assertTrue(result.isEmpty());
    }
}
