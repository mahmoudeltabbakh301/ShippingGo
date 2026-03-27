package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByName(String name);

    java.util.List<Store> findByAdminId(Long adminId);

    boolean existsByAdminIdAndId(Long adminId, Long id);
}
