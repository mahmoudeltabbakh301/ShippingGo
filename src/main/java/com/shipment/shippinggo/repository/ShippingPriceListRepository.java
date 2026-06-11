package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.ShippingPriceList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShippingPriceListRepository extends JpaRepository<ShippingPriceList, Long> {

    List<ShippingPriceList> findByOrganizationIdAndActiveTrue(Long organizationId);

    List<ShippingPriceList> findByOrganizationId(Long organizationId);

    Optional<ShippingPriceList> findByOrganizationIdAndIsDefaultTrue(Long organizationId);

    boolean existsByOrganizationIdAndNameAndIdNot(Long organizationId, String name, Long id);
}
