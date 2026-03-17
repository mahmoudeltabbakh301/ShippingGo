package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {
    List<OrderStatusHistory> findByOrderOrderByChangedAtDesc(Order order);

    List<OrderStatusHistory> findByOrderIdOrderByChangedAtDesc(Long orderId);

    void deleteByOrder(Order order);
}
