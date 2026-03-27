package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByAdmin(User admin);

    List<Company> findByAdminId(Long adminId);

    boolean existsByAdminIdAndId(Long adminId, Long id);

}
