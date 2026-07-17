package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.OrderEvent;
import com.shipment.shippinggo.enums.OrderEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByActionDateDesc(Long orderId);

    void deleteByOrderId(Long orderId);

    // === New typed event queries ===

    boolean existsByOrderIdAndEventType(Long orderId, OrderEventType eventType);

    List<OrderEvent> findByOrderIdAndEventType(Long orderId, OrderEventType eventType);

    List<OrderEvent> findByOrderIdAndEventTypeOrderByActionDateDesc(Long orderId, OrderEventType eventType);

    boolean existsByOrderIdAndEventTypeAndOrganizationId(Long orderId, OrderEventType eventType, Long organizationId);

}
