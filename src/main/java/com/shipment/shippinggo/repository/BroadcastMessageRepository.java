package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.BroadcastMessage;
import com.shipment.shippinggo.enums.BroadcastType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessage, Long> {

    List<BroadcastMessage> findAllByOrderBySentAtDesc();

    List<BroadcastMessage> findByActiveTrueOrderBySentAtDesc();

    List<BroadcastMessage> findByTypeAndActiveTrueOrderBySentAtDesc(BroadcastType type);
}
