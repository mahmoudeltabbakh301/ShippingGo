package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.VirtualOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VirtualOfficeRepository extends JpaRepository<VirtualOffice, Long> {
    List<VirtualOffice> findByParentOrganization(Organization parentOrganization);

    List<VirtualOffice> findByParentOrganizationId(Long parentOrganizationId);

    boolean existsByNameAndParentOrganizationId(String name, Long parentOrganizationId);

    List<VirtualOffice> findByAdminId(Long adminId);
}
