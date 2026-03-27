package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import com.shipment.shippinggo.repository.OrderAssignmentRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountSummaryServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private BusinessDayRepository businessDayRepository;
    @Mock
    private OrderAssignmentRepository orderAssignmentRepository;
    @Mock
    private TransactionService transactionService;
    @Mock
    private CommissionService commissionService;
    @Mock
    private VirtualOfficeRepository virtualOfficeRepository;

    private AccountSummaryService accountSummaryService;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        accountSummaryService = new AccountSummaryService(orderRepository, businessDayRepository,
                orderAssignmentRepository, transactionService, commissionService, virtualOfficeRepository);

        testOrg = new Company();
        testOrg.setId(1L);
        testOrg.setName("Test Org");
    }

    @Test
    void getOrganizationAccountSummary_EmptyOrders_ReturnsZeroSummary() {
        when(orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        AccountSummaryDTO result = accountSummaryService.getOrganizationAccountSummary(testOrg);

        assertNotNull(result);
        assertEquals(0, result.getDeliveredOrders());
        assertEquals(0, result.getReturnedOrders());
    }

    @Test
    void getOrganizationAccountSummary_WithDeliveredOrders() {
        Order delivered1 = new Order();
        delivered1.setStatus(OrderStatus.DELIVERED);
        delivered1.setAmount(new BigDecimal("100"));

        Order delivered2 = new Order();
        delivered2.setStatus(OrderStatus.DELIVERED);
        delivered2.setAmount(new BigDecimal("200"));

        Order waiting = new Order();
        waiting.setStatus(OrderStatus.WAITING);
        waiting.setAmount(new BigDecimal("50"));

        when(orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(delivered1, delivered2, waiting));

        AccountSummaryDTO result = accountSummaryService.getOrganizationAccountSummary(testOrg);

        assertEquals(2, result.getDeliveredOrders());
        assertEquals(0, result.getReturnedOrders());
        assertEquals(1, result.getOtherOrders());
        assertEquals(new BigDecimal("300"), result.getDeliveredAmount());
    }

    @Test
    void getOrganizationAccountSummary_WithRefusedOrders() {
        Order refused = new Order();
        refused.setStatus(OrderStatus.REFUSED);
        refused.setRejectionPayment(new BigDecimal("25"));

        when(orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(refused));

        AccountSummaryDTO result = accountSummaryService.getOrganizationAccountSummary(testOrg);

        assertEquals(1, result.getReturnedOrders());
        assertEquals(new BigDecimal("25"), result.getReturnedAmount());
    }

    @Test
    void getOrganizationAccountSummary_NullAmounts_DefaultsToZero() {
        Order delivered = new Order();
        delivered.setStatus(OrderStatus.DELIVERED);
        delivered.setAmount(null);

        Order refused = new Order();
        refused.setStatus(OrderStatus.REFUSED);
        refused.setRejectionPayment(null);

        when(orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(delivered, refused));

        AccountSummaryDTO result = accountSummaryService.getOrganizationAccountSummary(testOrg);

        assertEquals(BigDecimal.ZERO, result.getDeliveredAmount());
        assertEquals(BigDecimal.ZERO, result.getReturnedAmount());
    }

    @Test
    void getCourierAccountSummary_EmptyOrders_ReturnsZeroSummary() {
        User courier = new User();
        courier.setId(1L);

        when(orderRepository.findByAssignedToCourierIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        AccountSummaryDTO result = accountSummaryService.getCourierAccountSummary(courier, testOrg);

        assertNotNull(result);
        assertEquals(0, result.getDeliveredOrders());
    }
}
