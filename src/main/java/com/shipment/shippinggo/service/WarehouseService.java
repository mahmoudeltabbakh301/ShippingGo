package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.BusinessDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final BusinessDayRepository businessDayRepository;

    public WarehouseService(OrderRepository orderRepository,
            @org.springframework.context.annotation.Lazy OrderService orderService,
            BusinessDayRepository businessDayRepository) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.businessDayRepository = businessDayRepository;
    }

    /**
     * جلب أوردرات المخزن الأساسي ليوم عمل (حالة WAITING + المؤكد استلامها)
     */
    public List<Order> getWarehouseOrders(Long businessDayId, String search, String code,
            Long courierId, Long officeId, com.shipment.shippinggo.enums.OrderStatus status,
            Long incomingFromId, Long outgoingToId, org.springframework.data.domain.Pageable pageable) {

        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return List.of();
        Long orgId = bd.getOrganization().getId();

        // جلب أوردرات يوم العمل بالفلاتر كاملة (تجنب N+1 والفلترة اليدوية للمنظمات)
        List<Order> allOrders = orderService.getOrdersByBusinessDayWithFullFilters(
                businessDayId, orgId, search, code, courierId, incomingFromId, outgoingToId, status, null, false, org.springframework.data.domain.Pageable.unpaged());

        java.util.Map<Long, java.util.Map<String, Boolean>> returnCtx = orderService.getOrderReturnContext(allOrders,
                orgId);

        List<Order> filtered = allOrders.stream()
                .filter(o -> {
                    // أوردرات في حالة WAITING = داخل المخزن الأساسي
                    if (o.getStatus() == OrderStatus.WAITING) {
                        return true;
                    }
                    // أوردرات ملغية/مرفوضة/مؤجلة تعتمد على سياق المرتجع
                    if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REFUSED
                            || o.getStatus() == OrderStatus.PARTIAL_DELIVERY
                            || o.getStatus() == OrderStatus.DEFERRED) {
                        return returnCtx.getOrDefault(o.getId(), java.util.Collections.emptyMap())
                                .getOrDefault("isInternal", false);
                    }
                    return false;
                })
                .toList();

        if (pageable != null && pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            if (start >= filtered.size()) return List.of();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            return filtered.subList(start, end);
        }
        return filtered;
    }

    /**
     * جلب أوردرات المخزن الخارجي ليوم عمل
     * (IN_TRANSIT + CANCELLED/REFUSED بدون تأكيد استلام كامل + DELIVERED)
     */
    public List<Order> getExternalWarehouseOrders(Long businessDayId, String search, String code,
            Long courierId, Long officeId, com.shipment.shippinggo.enums.OrderStatus status,
            Long incomingFromId, Long outgoingToId, org.springframework.data.domain.Pageable pageable) {

        BusinessDay bd = businessDayRepository.findById(businessDayId).orElse(null);
        if (bd == null)
            return List.of();
        Long orgId = bd.getOrganization().getId();

        List<Order> allOrders = orderService.getOrdersByBusinessDayWithFullFilters(
                businessDayId, orgId, search, code, courierId, incomingFromId, outgoingToId, status, null, false, org.springframework.data.domain.Pageable.unpaged());

        java.util.Map<Long, java.util.Map<String, Boolean>> returnCtx = orderService.getOrderReturnContext(allOrders,
                orgId);

        List<Order> filtered = allOrders.stream()
                .filter(o -> {
                    // في الطريق = في المخزن الخارجي
                    if (o.getStatus() == OrderStatus.IN_TRANSIT) {
                        return true;
                    }
                    // تم التسليم = في المخزن الخارجي (للعرض)
                    if (o.getStatus() == OrderStatus.DELIVERED) {
                        return true;
                    }
                    // ملغي/مرفوض/مؤجل يعتمد على سياق المرتجع
                    if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.REFUSED
                            || o.getStatus() == OrderStatus.PARTIAL_DELIVERY
                            || o.getStatus() == OrderStatus.DEFERRED) {
                        return returnCtx.getOrDefault(o.getId(), java.util.Collections.emptyMap())
                                .getOrDefault("isExternal", false);
                    }
                    return false;
                })
                .toList();

        if (pageable != null && pageable.isPaged()) {
            int start = (int) pageable.getOffset();
            if (start >= filtered.size()) return List.of();
            int end = Math.min((start + pageable.getPageSize()), filtered.size());
            return filtered.subList(start, end);
        }
        return filtered;
    }

    /**
     * تأكيد استلام أوردر مرتجع من منظمة محددة وبناءً على تواجدها في دورة حياة الطلب
     */
    @Transactional
    public void confirmWarehouseReceipt(Long orderId, Long organizationId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        // تحقق أن الأوردر في حالة ملغي أو مرفوض أو مؤجل
        if (order.getStatus() != OrderStatus.CANCELLED && order.getStatus() != OrderStatus.REFUSED
                && order.getStatus() != OrderStatus.DEFERRED
                && order.getStatus() != OrderStatus.PARTIAL_DELIVERY) {
            throw new BusinessLogicException(
                    "لا يمكن تأكيد الاستلام إلا للأوردرات الملغية أو المرفوضة أو المؤجلة أو ذات الاستلام الجزئي");
        }

        // تحديد هل المنظمة هي المالكة أو المسند إليها
        if (order.getOwnerOrganization() != null
                && order.getOwnerOrganization().getId().equals(organizationId)) {
            order.setWarehouseReceiptConfirmedByOwner(true);
        } else if (order.getAssignedToOrganization() != null
                && order.getAssignedToOrganization().getId().equals(organizationId)) {
            order.setWarehouseReceiptConfirmedByAssignee(true);
        } else {
            throw new BusinessLogicException("المنظمة غير مرتبطة بهذا الأوردر");
        }

        orderRepository.save(order);
    }

    /**
     * التحقق مما إذا تم تأكيد الاستلام بالكامل
     */
    public boolean isReceiptFullyConfirmed(Order order) {
        // إذا لم يكن مسند لمنظمة أخرى، يكفي تأكيد المالك
        if (order.getAssignedToOrganization() == null) {
            return order.isWarehouseReceiptConfirmedByOwner();
        }
        // إذا كان مسند، يجب تأكيد الطرفين
        return order.isWarehouseReceiptConfirmedByOwner()
                && order.isWarehouseReceiptConfirmedByAssignee();
    }

    /**
     * التحقق هل المنظمة أكدت استلام أوردر معين
     */
    public boolean hasOrganizationConfirmedReceipt(Order order, Long organizationId) {
        if (order.getOwnerOrganization() != null
                && order.getOwnerOrganization().getId().equals(organizationId)) {
            return order.isWarehouseReceiptConfirmedByOwner();
        }
        if (order.getAssignedToOrganization() != null
                && order.getAssignedToOrganization().getId().equals(organizationId)) {
            return order.isWarehouseReceiptConfirmedByAssignee();
        }
        return false;
    }
}
