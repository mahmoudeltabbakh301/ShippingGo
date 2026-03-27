package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.ShipmentRequest;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRequestRepository extends JpaRepository<ShipmentRequest, Long> {
    List<ShipmentRequest> findByRequester(User requester);

    List<ShipmentRequest> findByOrganizationId(Long organizationId);

    @Query("SELECT sr FROM ShipmentRequest sr WHERE sr.organization.id = :orgId AND CAST(sr.createdAt AS LocalDate) = :date")
    List<ShipmentRequest> findByOrganizationIdAndDate(
            @org.springframework.data.repository.query.Param("orgId") Long orgId,
            @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}
