package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    List<OrderEvent> findByOrderIdOrderByActionDateDesc(Long orderId);

    void deleteByOrderId(Long orderId);

}
