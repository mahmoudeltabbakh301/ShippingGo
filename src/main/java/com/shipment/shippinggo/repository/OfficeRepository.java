package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Office;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfficeRepository extends JpaRepository<Office, Long> {
    Optional<Office> findByAdmin(User admin);

    List<Office> findByAdminId(Long adminId);

    boolean existsByAdminIdAndId(Long adminId, Long id);

    List<Office> findByParentCompany(Company company);

    List<Office> findByParentCompanyId(Long companyId);

}
