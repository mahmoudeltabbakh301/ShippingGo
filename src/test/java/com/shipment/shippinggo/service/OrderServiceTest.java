package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderCreationService orderCreationService;
    @Mock
    private OrderAssignmentService orderAssignmentService;
    @Mock
    private OrderStatusService orderStatusService;
    @Mock
    private OrderQueryService orderQueryService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderCreationService, orderAssignmentService,
                orderStatusService, orderQueryService);
    }

    // === Creation Delegation ===

    @Test
    void saveOrder_DelegatesToCreationService() {
        Order order = new Order();
        when(orderCreationService.saveOrder(order)).thenReturn(order);

        Order result = orderService.saveOrder(order);
        assertEquals(order, result);
        verify(orderCreationService).saveOrder(order);
    }

    @Test
    void createOrder_DelegatesToCreationService() {
        OrderDto dto = new OrderDto();
        User user = new User();
        Organization org = new Company();
        Order expected = new Order();
        when(orderCreationService.createOrder(dto, user, org)).thenReturn(expected);

        Order result = orderService.createOrder(dto, user, org);
        assertEquals(expected, result);
    }

    @Test
    void updateOrderDetails_DelegatesToCreationService() {
        OrderDto dto = new OrderDto();
        User user = new User();
        Order expected = new Order();
        when(orderCreationService.updateOrderDetails(1L, dto, user)).thenReturn(expected);

        Order result = orderService.updateOrderDetails(1L, dto, user);
        assertEquals(expected, result);
    }

    @Test
    void deleteOrder_DelegatesToCreationService() {
        User user = new User();
        orderService.deleteOrder(1L, user);
        verify(orderCreationService).deleteOrder(1L, user);
    }

    // === Assignment Delegation ===

    @Test
    void assignToOrganization_ById_DelegatesToAssignmentService() {
        User user = new User();
        Order expected = new Order();
        when(orderAssignmentService.assignToOrganization(1L, 2L, user)).thenReturn(expected);

        Order result = orderService.assignToOrganization(1L, 2L, user);
        assertEquals(expected, result);
    }

    @Test
    void assignToCourier_DelegatesToAssignmentService() {
        User user = new User();
        Order expected = new Order();
        when(orderAssignmentService.assignToCourier(1L, 5L, user)).thenReturn(expected);

        Order result = orderService.assignToCourier(1L, 5L, user);
        assertEquals(expected, result);
    }

    @Test
    void bulkAssignToOrganization_DelegatesToAssignmentService() {
        List<Long> ids = List.of(1L, 2L, 3L);
        User user = new User();
        orderService.bulkAssignToOrganization(ids, 10L, user);
        verify(orderAssignmentService).bulkAssignToOrganization(ids, 10L, user);
    }

    // === Query Delegation ===

    @Test
    void getOrdersByBusinessDay_DelegatesToQueryService() {
        List<Order> expected = List.of(new Order());
        when(orderQueryService.getOrdersByBusinessDay(1L)).thenReturn(expected);

        List<Order> result = orderService.getOrdersByBusinessDay(1L);
        assertEquals(expected, result);
    }
}
