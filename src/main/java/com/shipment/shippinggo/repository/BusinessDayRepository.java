package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessDayRepository extends JpaRepository<BusinessDay, Long> {
    Optional<BusinessDay> findByOrganizationAndDateAndIsCustodyFalse(Organization organization, LocalDate date);

    Optional<BusinessDay> findByOrganizationIdAndDateAndIsCustodyFalse(Long organizationId, LocalDate date);

    List<BusinessDay> findByOrganizationAndIsCustodyFalseOrderByDateDesc(Organization organization);

    List<BusinessDay> findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(Long organizationId);

    Optional<BusinessDay> findByOrganizationAndActiveTrueAndIsCustodyFalse(Organization organization);

    List<BusinessDay> findByOrganizationIdAndActiveTrueAndIsCustodyFalseOrderByDateDesc(Long organizationId);

    org.springframework.data.domain.Page<BusinessDay> findByOrganizationIdAndIsCustodyFalseOrderByDateDesc(Long organizationId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<BusinessDay> findByOrganizationIdAndActiveTrueAndIsCustodyFalseOrderByDateDesc(Long organizationId, org.springframework.data.domain.Pageable pageable);

    // Custody specific
    Optional<BusinessDay> findByOrganizationIdAndDateAndIsCustodyTrue(Long organizationId, LocalDate date);

    List<BusinessDay> findByOrganizationIdAndIsCustodyTrueOrderByDateDesc(Long organizationId);

    List<BusinessDay> findByOrganizationIdAndActiveTrueAndIsCustodyTrueOrderByDateDesc(Long organizationId);
}
