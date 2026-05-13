package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.GovernorateRoutingRule;
import com.shipment.shippinggo.enums.Governorate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GovernorateRoutingRuleRepository extends JpaRepository<GovernorateRoutingRule, Long> {

    Optional<GovernorateRoutingRule> findByStoreIdAndGovernorateAndActiveTrue(Long storeId, Governorate governorate);

    List<GovernorateRoutingRule> findByStoreIdOrderByGovernorate(Long storeId);

    boolean existsByStoreIdAndGovernorate(Long storeId, Governorate governorate);

    void deleteByStoreIdAndGovernorate(Long storeId, Governorate governorate);
}
