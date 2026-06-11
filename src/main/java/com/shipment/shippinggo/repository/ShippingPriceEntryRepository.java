package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.ShippingPriceEntry;
import com.shipment.shippinggo.enums.Governorate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShippingPriceEntryRepository extends JpaRepository<ShippingPriceEntry, Long> {

    List<ShippingPriceEntry> findByPriceListIdAndActiveTrue(Long priceListId);

    List<ShippingPriceEntry> findByPriceListId(Long priceListId);

    Optional<ShippingPriceEntry> findByPriceListIdAndGovernorate(Long priceListId, Governorate governorate);

    @Query("SELECT e FROM ShippingPriceEntry e WHERE e.priceList.organization.id = :orgId " +
           "AND e.priceList.isDefault = true AND e.governorate = :gov AND e.active = true AND e.priceList.active = true")
    Optional<ShippingPriceEntry> findDefaultPriceForOrg(@Param("orgId") Long orgId, @Param("gov") Governorate governorate);

    void deleteByPriceListId(Long priceListId);
}
