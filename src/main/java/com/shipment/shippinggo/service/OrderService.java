package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.annotation.LogSensitiveOperation;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Facade service for Order operations.
 * Delegates all logic to specifically focused Order services.
 */
@Service
public class OrderService {

    private final OrderCreationService orderCreationService;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderStatusService orderStatusService;
    private final OrderQueryService orderQueryService;

    public OrderService(OrderCreationService orderCreationService,
            OrderAssignmentService orderAssignmentService,
            OrderStatusService orderStatusService,
            OrderQueryService orderQueryService) {
        this.orderCreationService = orderCreationService;
        this.orderAssignmentService = orderAssignmentService;
        this.orderStatusService = orderStatusService;
        this.orderQueryService = orderQueryService;
    }

    // --- OrderCreationService Delegation ---
    // حفظ وإنشاء الطلبات

    public Order saveOrder(Order order) {
        return orderCreationService.saveOrder(order);
    }

    @LogSensitiveOperation(action = "CREATE_ORDER", entityName = "Order")
    public Order createOrder(OrderDto dto, User createdBy, Organization ownerOrganization) {
        return orderCreationService.createOrder(dto, createdBy, ownerOrganization);
    }

    @LogSensitiveOperation(action = "UPDATE_ORDER", entityName = "Order", logArguments = true)
    public Order updateOrderDetails(Long orderId, OrderDto dto, User updatedBy) {
        return orderCreationService.updateOrderDetails(orderId, dto, updatedBy);
    }

    @LogSensitiveOperation(action = "DELETE_ORDER", entityName = "Order", logArguments = true)
    public void deleteOrder(Long orderId, User user) {
        orderCreationService.deleteOrder(orderId, user);
    }

    public void deleteOrdersByBusinessDay(Long businessDayId, User user) {
        orderCreationService.deleteOrdersByBusinessDay(businessDayId, user);
    }

    // --- OrderAssignmentService Delegation ---
    // عمليات الإسناد إلى المنظمات والمندوبين

    @LogSensitiveOperation(action = "ASSIGN_ORDER_TO_ORG", entityName = "Order", logArguments = true)
    public Order assignToOrganization(Long orderId, Organization targetOrg, User assignedBy) {
        return orderAssignmentService.assignToOrganization(orderId, targetOrg, assignedBy);
    }

    public Order assignToOrganization(Long orderId, Long targetOrgId, User assignedBy) {
        return orderAssignmentService.assignToOrganization(orderId, targetOrgId, assignedBy);
    }

    public boolean canUserAccessOrder(User user, Order order) {
        return orderAssignmentService.canUserAccessOrder(user, order);
    }

    public void bulkAssignToOrganization(List<Long> orderIds, Long organizationId, User assignedBy) {
        orderAssignmentService.bulkAssignToOrganization(orderIds, organizationId, assignedBy);
    }

    @LogSensitiveOperation(action = "ASSIGN_ORDER_TO_COURIER", entityName = "Order", logArguments = true)
    public Order assignToCourier(Long orderId, Long courierId, User assignedBy) {
        return orderAssignmentService.assignToCourier(orderId, courierId, assignedBy);
    }

    public void bulkAssignToCourier(List<Long> orderIds, Long courierId, User assignedBy) {
        orderAssignmentService.bulkAssignToCourier(orderIds, courierId, assignedBy);
    }

    public Order unassignCourier(Long orderId, User removedBy) {
        return orderAssignmentService.unassignCourier(orderId, removedBy);
    }

    public Order unassignOrganization(Long orderId, User removedBy) {
        return orderAssignmentService.unassignOrganization(orderId, removedBy);
    }

    public Order acceptAssignment(Long orderId, User acceptedBy) {
        return orderAssignmentService.acceptAssignment(orderId, acceptedBy);
    }

    public Order rejectAssignment(Long orderId, User rejectedBy) {
        return orderAssignmentService.rejectAssignment(orderId, rejectedBy);
    }

    public Order assignToCustody(Long orderId, User assignedBy) {
        return orderAssignmentService.assignToCustody(orderId, assignedBy);
    }

    public Order removeFromCustody(Long orderId, User assignedBy) {
        return orderAssignmentService.removeFromCustody(orderId, assignedBy);
    }

    // --- OrderStatusService Delegation ---
    // تحديث حالات الطلب

    @LogSensitiveOperation(action = "UPDATE_ORDER_STATUS", entityName = "Order", logArguments = true)
    public Order updateStatus(Long orderId, OrderStatus newStatus, BigDecimal newAmount, User changedBy, String notes) {
        return orderStatusService.updateStatus(orderId, newStatus, newAmount, changedBy, notes);
    }

    @LogSensitiveOperation(action = "UPDATE_ORDER_STATUS_REJECTION", entityName = "Order", logArguments = true)
    public Order updateStatusWithRejectionPayment(Long orderId, OrderStatus newStatus, BigDecimal newAmount,
            BigDecimal rejectionPayment, User changedBy, String notes) {
        return orderStatusService.updateStatusWithRejectionPayment(orderId, newStatus, newAmount, rejectionPayment,
                changedBy, notes);
    }

    @LogSensitiveOperation(action = "UPDATE_ORDER_STATUS_ADVANCED", entityName = "Order", logArguments = true)
    public Order updateStatusAdvanced(Long orderId, OrderStatus newStatus, BigDecimal newAmount,
            BigDecimal rejectionPayment, Integer deliveredPieces, BigDecimal partialDeliveryAmount, User changedBy,
            String notes) {
        return orderStatusService.updateStatusAdvanced(orderId, newStatus, newAmount, rejectionPayment, deliveredPieces,
                partialDeliveryAmount, changedBy, notes);
    }

    public Order confirmReturn(Long orderId, User confirmedBy) {
        return orderStatusService.confirmReturn(orderId, confirmedBy);
    }

    // --- OrderQueryService Delegation ---
    // عمليات البحث والاستعلام عن الطلبات

    public Order getById(Long id) {
        return orderQueryService.getById(id);
    }

    public List<Order> getOrdersByOrganization(Long organizationId) {
        return orderQueryService.getOrdersByOrganization(organizationId);
    }

    public List<Order> searchOrders(Long organizationId, String query) {
        return orderQueryService.searchOrders(organizationId, query);
    }

    public List<Order> filterOrders(Long organizationId, String code, Long courierId, Long officeId, OrderStatus status,
            Governorate governorate) {
        return orderQueryService.filterOrders(organizationId, code, courierId, officeId, status, governorate);
    }

    public List<Order> getOrdersByBusinessDay(Long businessDayId) {
        return orderQueryService.getOrdersByBusinessDay(businessDayId);
    }

    public List<Order> getOrdersByBusinessDayWithFilters(Long businessDayId, String search, String code, Long courierId,
            Long officeId, OrderStatus status, Governorate governorate) {
        return orderQueryService.getOrdersByBusinessDayWithFilters(businessDayId, search, code, courierId, officeId,
                status, governorate);
    }

    public List<Order> getOrdersByBusinessDayWithFullFilters(Long businessDayId, Long orgId, String search, String code,
            Long courierId, Long incomingFromId, Long outgoingToId, OrderStatus status, Governorate governorate) {
        return orderQueryService.getOrdersByBusinessDayWithFullFilters(businessDayId, orgId, search, code, courierId,
                incomingFromId, outgoingToId, status, governorate);
    }

    public List<Order> getCustodyOrdersWithFilters(Long orgId, String search, String code, Long courierId,
            Long incomingFromId, Long outgoingToId, OrderStatus status, Governorate governorate) {
        return orderQueryService.getCustodyOrdersWithFilters(orgId, search, code, courierId, 
                incomingFromId, outgoingToId, status, governorate);
    }

    public Map<Long, Map<String, String>> getOrderChainContext(List<Order> orders, Long orgId) {
        return orderQueryService.getOrderChainContext(orders, orgId);
    }

    public List<Order> getOrdersAssignedToCourier(Long courierId) {
        return orderQueryService.getOrdersAssignedToCourier(courierId);
    }

    public List<Order> getOrdersByCreatedBy(Long createdById) {
        return orderQueryService.getOrdersByCreatedBy(createdById);
    }

    public List<Order> getOrdersByRecipientPhone(String phone) {
        return orderQueryService.getOrdersByRecipientPhone(phone);
    }

    public Order getOrderByCode(String code) {
        return orderQueryService.getOrderByCode(code);
    }

    public List<Order> getOrdersAssignedToCourierToday(Long courierId) {
        return orderQueryService.getOrdersAssignedToCourierToday(courierId);
    }

    public List<Order> getOrdersAssignedToCourierByDate(Long courierId, LocalDate date) {
        return orderQueryService.getOrdersAssignedToCourierByDate(courierId, date);
    }

    public long countCourierTodayOrdersByStatus(Long courierId, OrderStatus status) {
        return orderQueryService.countCourierTodayOrdersByStatus(courierId, status);
    }

    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return orderQueryService.getOrderHistory(orderId);
    }

    public long countByStatus(Long organizationId, OrderStatus status) {
        return orderQueryService.countByStatus(organizationId, status);
    }

    public long countCourierOrdersByStatus(Long courierId, OrderStatus status) {
        return orderQueryService.countCourierOrdersByStatus(courierId, status);
    }

    public List<Order> getOrdersAssignedToOrganization(Long organizationId) {
        return orderQueryService.getOrdersAssignedToOrganization(organizationId);
    }

    public List<Order> getOrdersAssignedToOrganizationToday(Long organizationId) {
        return orderQueryService.getOrdersAssignedToOrganizationToday(organizationId);
    }

    public List<Order> getOrdersAssignedToOrganizationByDate(Long organizationId, LocalDate date) {
        return orderQueryService.getOrdersAssignedToOrganizationByDate(organizationId, date);
    }

    public List<Order> getPendingAssignments(Long organizationId) {
        return orderQueryService.getPendingAssignments(organizationId);
    }

    public List<Order> getPendingAssignmentsByDate(Long organizationId, LocalDate date) {
        return orderQueryService.getPendingAssignmentsByDate(organizationId, date);
    }

    public Map<Long, Map<String, Boolean>> getOrderReturnContext(List<Order> orders, Long orgId) {
        return orderQueryService.getOrderReturnContext(orders, orgId);
    }

    public Map<String, Object> getReturnStatus(Long orderId) {
        return orderStatusService.getReturnStatus(orderId);
    }
}
