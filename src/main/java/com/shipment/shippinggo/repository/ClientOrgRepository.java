package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.ClientOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientOrgRepository extends JpaRepository<ClientOrg, Long> {
    List<ClientOrg> findByAdminId(Long adminId);
    boolean existsByAdminIdAndId(Long adminId, Long id);
}
