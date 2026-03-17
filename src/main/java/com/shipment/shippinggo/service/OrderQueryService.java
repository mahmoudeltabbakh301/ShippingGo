package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.*;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final BusinessDayRepository businessDayRepository;
    private final OrganizationRepository organizationRepository;
    private final OfficeRepository officeRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;

    public OrderQueryService(OrderRepository orderRepository,
            BusinessDayRepository businessDayRepository,
            OrganizationRepository organizationRepository,
            OfficeRepository officeRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository) {
        this.orderRepository = orderRepository;
        this.businessDayRepository = businessDayRepository;
        this.organizationRepository = organizationRepository;
        this.officeRepository = officeRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
    }

    // استرجاع الطلب عبر المُعرف
    public Order getById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    // الاستعلام عن طلبات المنظمة (الشركة الأساسية، الفروع، والمتاجر التابعة)
    public List<Order> getOrdersByOrganization(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId).orElse(null);
        if (org == null)
            return List.of();

        List<Order> ownedOrders = new java.util.ArrayList<>(
                orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(organizationId));

        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            List<Office> offices = officeRepository.findByParentCompanyId(organizationId);
            for (Office office : offices) {
                ownedOrders.addAll(orderRepository.findByOwnerOrganizationIdOrderByCreatedAtDesc(office.getId()));
            }
        }

        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            List<Order> createdOrders = orderRepository.findByCreatorOrganizationIdOrderByCreatedAtDesc(organizationId);
            ownedOrders.addAll(createdOrders);
        }

        List<Long> chainOrderIds = orderAssignmentRepository.findOrderIdsByOrganizationInChain(organizationId);
        if (!chainOrderIds.isEmpty()) {
            List<Order> chainOrders = orderRepository.findAllById(chainOrderIds);
            ownedOrders.addAll(chainOrders);
        }

        return ownedOrders.stream()
                .distinct()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    // بحث نصي للطلبات الخاصة بالمنظمة عبر الكود أو اسم المستلم أو رقم هاتفه
    public List<Order> searchOrders(Long organizationId, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getOrdersByOrganization(organizationId);
        }

        String lowerQuery = query.toLowerCase().trim();
        return getOrdersByOrganization(organizationId).stream()
                .filter(o -> (o.getCode() != null && o.getCode().toLowerCase().contains(lowerQuery)) ||
                        (o.getRecipientName() != null && o.getRecipientName().toLowerCase().contains(lowerQuery)) ||
                        (o.getRecipientPhone() != null && o.getRecipientPhone().contains(lowerQuery)) ||
                        (o.getRecipientAddress() != null && o.getRecipientAddress().toLowerCase().contains(lowerQuery)))
                .toList();
    }

    // تصفية الطلبات (فلترة) عبر الكود، المحافظة، المندوب، والفرع
    public List<Order> filterOrders(Long organizationId, String code, Long courierId, Long officeId,
            OrderStatus status, Governorate governorate) {
        if (code != null && code.trim().isEmpty())
            code = null;

        return orderRepository.findOrdersWithFilters(organizationId, code, courierId, officeId, status, governorate)
                .stream()
                .distinct()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public List<Order> getOrdersByBusinessDay(Long businessDayId) {
        return getOrdersByBusinessDayWithFilters(businessDayId, null, null, null, null, null, null);
    }

    // جلب طلبات يوم عمل محدد مع تطبيق فلاتر البحث المعقدة
    public List<Order> getOrdersByBusinessDayWithFilters(Long businessDayId, String search, String code,
            Long courierId, Long officeId, OrderStatus status, Governorate governorate) {
        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return List.of();

        if (search != null && search.trim().isEmpty())
            search = null;
        if (code != null && code.trim().isEmpty())
            code = null;

        List<Order> orders = orderRepository.findOrdersByBusinessDayWithFilters(businessDayId, search, code,
                courierId, officeId, status, governorate);

        return orders.stream()
                .distinct()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public List<Order> getOrdersByBusinessDayWithFullFilters(Long businessDayId, Long orgId, String search, String code,
            Long courierId, Long incomingFromId, Long outgoingToId, OrderStatus status, Governorate governorate) {
        return orderRepository.findOrdersByBusinessDayWithFullFilters(businessDayId, orgId, search, code, courierId, status,
                governorate, incomingFromId, outgoingToId);
    }

    public List<Order> getCustodyOrdersWithFilters(Long orgId, String search, String code, Long courierId,
            Long incomingFromId, Long outgoingToId, OrderStatus status, Governorate governorate) {
        if (search != null && search.trim().isEmpty())
            search = null;
        if (code != null && code.trim().isEmpty())
            code = null;

        List<Order> orders = orderRepository.findCustodyOrdersWithFilters(orgId, search, code,
                courierId, status, governorate, incomingFromId, outgoingToId);

        return orders.stream()
                .distinct()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    // تحديد سلسلة إسناد الطلبات (صادر وإلى أين، ووارد ومن أين)
    public java.util.Map<Long, java.util.Map<String, String>> getOrderChainContext(List<Order> orders, Long orgId) {
        java.util.Map<Long, java.util.Map<String, String>> contextMap = new java.util.HashMap<>();

        if (orders == null || orders.isEmpty())
            return contextMap;

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderAssignment> incoming = orderAssignmentRepository.findIncomingAssignments(orgId, orderIds);
        List<OrderAssignment> outgoing = orderAssignmentRepository.findOutgoingAssignments(orgId, orderIds);

        for (OrderAssignment oa : incoming) {
            Long oid = oa.getOrder().getId();
            contextMap.computeIfAbsent(oid, k -> new java.util.HashMap<>())
                    .put("incomingFrom", oa.getAssignerOrganization().getName());
        }

        for (OrderAssignment oa : outgoing) {
            Long oid = oa.getOrder().getId();
            contextMap.computeIfAbsent(oid, k -> new java.util.HashMap<>())
                    .put("outgoingTo", oa.getAssigneeOrganization().getName());
        }

        return contextMap;
    }

    public List<Order> getOrdersAssignedToCourier(Long courierId) {
        return orderRepository.findByAssignedToCourierIdOrderByCreatedAtDesc(courierId);
    }

    public List<Order> getOrdersByCreatedBy(Long createdById) {
        return orderRepository.findByCreatedByIdOrderByCreatedAtDesc(createdById);
    }

    public List<Order> getOrdersAssignedToCourierToday(Long courierId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        return orderRepository.findByAssignedToCourierIdAndCourierAssignmentDate(courierId, startOfDay, startOfNextDay);
    }

    public List<Order> getOrdersAssignedToCourierByDate(Long courierId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();
        return orderRepository.findByAssignedToCourierIdAndCourierAssignmentDate(courierId, startOfDay, startOfNextDay);
    }

    public long countCourierTodayOrdersByStatus(Long courierId, OrderStatus status) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
        return orderRepository.countByAssignedToCourierIdAndCourierAssignmentDateAndStatus(
                courierId, startOfDay, startOfNextDay, status);
    }

    public List<OrderStatusHistory> getOrderHistory(Long orderId) {
        return orderStatusHistoryRepository.findByOrderIdOrderByChangedAtDesc(orderId);
    }

    public long countByStatus(Long organizationId, OrderStatus status) {
        return orderRepository.countByOwnerOrganizationIdAndStatus(organizationId, status);
    }

    public long countCourierOrdersByStatus(Long courierId, OrderStatus status) {
        return orderRepository.countByAssignedToCourierIdAndStatus(courierId, status);
    }

    public List<Order> getOrdersAssignedToOrganization(Long organizationId) {
        return orderRepository.findByAssignedToOrganizationId(organizationId)
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public List<Order> getOrdersAssignedToOrganizationToday(Long organizationId) {
        return getOrdersAssignedToOrganizationByDate(organizationId, LocalDate.now());
    }

    public List<Order> getOrdersAssignedToOrganizationByDate(Long organizationId, LocalDate date) {
        return orderRepository.findByAssignedToOrganizationIdAndAssignmentDate(organizationId, date)
                .stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public List<Order> getPendingAssignments(Long organizationId) {
        return orderRepository.findByAssignedToOrganizationId(organizationId)
                .stream()
                .filter(o -> !o.isAssignmentAccepted())
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    public List<Order> getPendingAssignmentsByDate(Long organizationId, LocalDate date) {
        return orderRepository.findByAssignedToOrganizationIdAndAssignmentDate(organizationId, date)
                .stream()
                .filter(o -> !o.isAssignmentAccepted())
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .toList();
    }

    // الحصول على سياق إرجاع الطلب المرفوض وتتبع مستوى الإرجاع لتحديد إمكانية
    // التوضيح
    public java.util.Map<Long, java.util.Map<String, Boolean>> getOrderReturnContext(List<Order> orders, Long orgId) {
        java.util.Map<Long, java.util.Map<String, Boolean>> contextMap = new java.util.HashMap<>();

        if (orders == null || orders.isEmpty())
            return contextMap;

        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderAssignment> allAssignments = orderAssignmentRepository.findByOrderIdInOrderByLevelAsc(orderIds);

        java.util.Map<Long, List<OrderAssignment>> orderAssignmentsMap = new java.util.HashMap<>();
        for (OrderAssignment oa : allAssignments) {
            orderAssignmentsMap.computeIfAbsent(oa.getOrder().getId(), k -> new java.util.ArrayList<>()).add(oa);
        }

        for (Order order : orders) {
            java.util.Map<String, Boolean> ctx = new java.util.HashMap<>();
            ctx.put("canConfirm", false);
            ctx.put("hasConfirmed", false);
            ctx.put("isInternal", false);
            ctx.put("isExternal", false);

            if (order.getStatus() != OrderStatus.REFUSED && order.getStatus() != OrderStatus.CANCELLED
                    && order.getStatus() != OrderStatus.PARTIAL_DELIVERY
                    && order.getStatus() != OrderStatus.DEFERRED) {
                contextMap.put(order.getId(), ctx);
                continue;
            }

            List<OrderAssignment> chain = orderAssignmentsMap.getOrDefault(order.getId(),
                    java.util.Collections.emptyList());

            boolean isOwner = order.getOwnerOrganization().getId().equals(orgId);
            boolean isAssigneeInChain = false;
            boolean isAssignerInChain = false;
            boolean hasConfirmedAsAssignee = false;

            for (OrderAssignment oa : chain) {
                if (oa.getAssigneeOrganization().getId().equals(orgId)) {
                    isAssigneeInChain = true;
                    hasConfirmedAsAssignee = oa.isReturnConfirmed();
                }
                if (oa.getAssignerOrganization().getId().equals(orgId)) {
                    isAssignerInChain = true;
                }
            }

            if (isOwner) {
                ctx.put("hasConfirmed", order.isReturnedToOwner());
            } else if (isAssigneeInChain) {
                ctx.put("hasConfirmed", hasConfirmedAsAssignee);
            } else if (isAssignerInChain) {
                ctx.put("hasConfirmed", false);
            }

            if (isOwner || isAssigneeInChain || isAssignerInChain || chain.isEmpty()) {
                ctx.put("isExternal", true);
                if (isOwner && order.isReturnedToOwner()) {
                    ctx.put("isInternal", true);
                }
            }

            if (chain.isEmpty()) {
                if (isOwner && !order.isReturnedToOwner()) {
                    ctx.put("canConfirm", true);
                }
            } else {
                OrderAssignment pendingAssignment = null;
                for (int i = chain.size() - 1; i >= 0; i--) {
                    if (!chain.get(i).isReturnConfirmed()) {
                        pendingAssignment = chain.get(i);
                        break;
                    }
                }

                if (pendingAssignment != null) {
                    if (pendingAssignment.getAssigneeOrganization().getId().equals(orgId)) {
                        ctx.put("canConfirm", true);
                    }
                } else {
                    if (isOwner && !order.isReturnedToOwner()) {
                        ctx.put("canConfirm", true);
                    }
                }
            }

            contextMap.put(order.getId(), ctx);
        }

        return contextMap;
    }

    int getAssignmentLevel(List<OrderAssignment> chain, Long orgId) {
        for (OrderAssignment oa : chain) {
            if (oa.getAssigneeOrganization().getId().equals(orgId)) {
                return oa.getLevel();
            }
        }
        return -1;
    }

    boolean isReadyForNextLevel(Order order, List<OrderAssignment> chain, Long orgId) {
        if (order.getOwnerOrganization().getId().equals(orgId)) {
            return order.isReturnedToOwner();
        }
        for (int i = 0; i < chain.size(); i++) {
            OrderAssignment oa = chain.get(i);
            if (oa.getAssigneeOrganization().getId().equals(orgId)) {
                if (oa.isReturnConfirmed()) {
                    if (i == 0) {
                        return !order.isReturnedToOwner();
                    } else {
                        return !chain.get(i - 1).isReturnConfirmed();
                    }
                }
                return false;
            }
        }
        return false;
    }
}
