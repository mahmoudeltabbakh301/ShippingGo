package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private BusinessDayRepository businessDayRepository;

    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        warehouseService = new WarehouseService(orderRepository, orderService, businessDayRepository);
    }

    private Organization createTestOrg(Long id) {
        Company org = new Company();
        org.setId(id);
        return org;
    }

    // === confirmWarehouseReceipt ===

    @Test
    void confirmWarehouseReceipt_OrderNotFound_Throws() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> warehouseService.confirmWarehouseReceipt(99L, 1L));
    }

    @Test
    void confirmWarehouseReceipt_WrongStatus_Throws() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessLogicException.class,
                () -> warehouseService.confirmWarehouseReceipt(1L, 1L));
    }

    @Test
    void confirmWarehouseReceipt_ByOwner_SetsOwnerFlag() {
        Organization ownerOrg = createTestOrg(1L);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CANCELLED);
        order.setOwnerOrganization(ownerOrg);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        warehouseService.confirmWarehouseReceipt(1L, 1L);

        assertTrue(order.isWarehouseReceiptConfirmedByOwner());
        verify(orderRepository).save(order);
    }

    @Test
    void confirmWarehouseReceipt_ByAssignee_SetsAssigneeFlag() {
        Organization ownerOrg = createTestOrg(1L);
        Organization assignedOrg = createTestOrg(2L);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.REFUSED);
        order.setOwnerOrganization(ownerOrg);
        order.setAssignedToOrganization(assignedOrg);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        warehouseService.confirmWarehouseReceipt(1L, 2L);

        assertTrue(order.isWarehouseReceiptConfirmedByAssignee());
        verify(orderRepository).save(order);
    }

    @Test
    void confirmWarehouseReceipt_UnrelatedOrg_Throws() {
        Organization ownerOrg = createTestOrg(1L);

        Order order = new Order();
        order.setId(1L);
        order.setStatus(OrderStatus.CANCELLED);
        order.setOwnerOrganization(ownerOrg);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(BusinessLogicException.class,
                () -> warehouseService.confirmWarehouseReceipt(1L, 99L));
    }

    // === isReceiptFullyConfirmed ===

    @Test
    void isReceiptFullyConfirmed_NoAssignee_OnlyOwnerNeeded() {
        Order order = new Order();
        order.setWarehouseReceiptConfirmedByOwner(true);
        order.setAssignedToOrganization(null);

        assertTrue(warehouseService.isReceiptFullyConfirmed(order));
    }

    @Test
    void isReceiptFullyConfirmed_WithAssignee_BothNeeded() {
        Organization assignedOrg = createTestOrg(2L);
        Order order = new Order();
        order.setAssignedToOrganization(assignedOrg);
        order.setWarehouseReceiptConfirmedByOwner(true);
        order.setWarehouseReceiptConfirmedByAssignee(false);

        assertFalse(warehouseService.isReceiptFullyConfirmed(order));
    }

    @Test
    void isReceiptFullyConfirmed_WithAssignee_BothConfirmed() {
        Organization assignedOrg = createTestOrg(2L);
        Order order = new Order();
        order.setAssignedToOrganization(assignedOrg);
        order.setWarehouseReceiptConfirmedByOwner(true);
        order.setWarehouseReceiptConfirmedByAssignee(true);

        assertTrue(warehouseService.isReceiptFullyConfirmed(order));
    }

    // === hasOrganizationConfirmedReceipt ===

    @Test
    void hasOrganizationConfirmedReceipt_OwnerConfirmed() {
        Organization ownerOrg = createTestOrg(1L);
        Order order = new Order();
        order.setOwnerOrganization(ownerOrg);
        order.setWarehouseReceiptConfirmedByOwner(true);

        assertTrue(warehouseService.hasOrganizationConfirmedReceipt(order, 1L));
    }

    @Test
    void hasOrganizationConfirmedReceipt_UnrelatedOrg_ReturnsFalse() {
        Organization ownerOrg = createTestOrg(1L);
        Order order = new Order();
        order.setOwnerOrganization(ownerOrg);

        assertFalse(warehouseService.hasOrganizationConfirmedReceipt(order, 99L));
    }

    // === getWarehouseOrders / getExternalWarehouseOrders ===

    @Test
    void getWarehouseOrders_NullBusinessDay_ReturnsEmpty() {
        when(businessDayRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(warehouseService.getWarehouseOrders(99L, null, null, null, null, null, null, null).isEmpty());
    }

    @Test
    void getExternalWarehouseOrders_NullBusinessDay_ReturnsEmpty() {
        when(businessDayRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(
                warehouseService.getExternalWarehouseOrders(99L, null, null, null, null, null, null, null).isEmpty());
    }
}
