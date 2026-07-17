package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.AssignmentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentPermissionRepository extends JpaRepository<AssignmentPermission, Long> {

    /**
     * التحقق السريع من وجود صلاحية إسناد بين منظمتين.
     */
    boolean existsBySourceOrganizationIdAndTargetOrganizationIdAndActiveTrue(
            Long sourceOrgId, Long targetOrgId);

    /**
     * جلب كل الصلاحيات النشطة لمنظمة مُسنِدة.
     */
    List<AssignmentPermission> findBySourceOrganizationIdAndActiveTrue(Long sourceOrgId);

    /**
     * جلب كل الصلاحيات النشطة لمنظمة مُسنَد إليها.
     */
    List<AssignmentPermission> findByTargetOrganizationIdAndActiveTrue(Long targetOrgId);

    /**
     * التحقق من وجود صلاحية (نشطة أو غير نشطة) بين منظمتين.
     */
    java.util.Optional<AssignmentPermission> findBySourceOrganizationIdAndTargetOrganizationId(
            Long sourceOrgId, Long targetOrgId);
}
