package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.CourierZoneAssignment;
import com.shipment.shippinggo.enums.Governorate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourierZoneAssignmentRepository extends JpaRepository<CourierZoneAssignment, Long> {

    List<CourierZoneAssignment> findByOrganizationIdAndActiveTrue(Long organizationId);

    List<CourierZoneAssignment> findByCourierIdAndOrganizationIdAndActiveTrue(Long courierId, Long organizationId);

    // بحث عن مندوب مسؤول عن محافظة + مركز + منطقة (exact match)
    @Query("SELECT c FROM CourierZoneAssignment c WHERE c.organization.id = :orgId " +
           "AND c.governorate = :gov AND c.center = :center AND c.area = :area AND c.active = true")
    Optional<CourierZoneAssignment> findByOrgAndGovAndCenterAndArea(
            @Param("orgId") Long orgId, @Param("gov") Governorate governorate,
            @Param("center") String center, @Param("area") String area);

    // بحث عن مندوب مسؤول عن محافظة + مركز (بدون منطقة)
    @Query("SELECT c FROM CourierZoneAssignment c WHERE c.organization.id = :orgId " +
           "AND c.governorate = :gov AND c.center = :center AND c.area IS NULL AND c.active = true")
    Optional<CourierZoneAssignment> findByOrgAndGovAndCenter(
            @Param("orgId") Long orgId, @Param("gov") Governorate governorate, @Param("center") String center);

    // بحث عن مندوب مسؤول عن محافظة + منطقة (legacy district)
    @Query("SELECT c FROM CourierZoneAssignment c WHERE c.organization.id = :orgId " +
           "AND c.governorate = :gov AND c.district = :district AND c.active = true")
    Optional<CourierZoneAssignment> findByOrgAndGovAndDistrict(
            @Param("orgId") Long orgId, @Param("gov") Governorate governorate, @Param("district") String district);

    // بحث عن مندوب مسؤول عن محافظة كاملة (fallback)
    @Query("SELECT c FROM CourierZoneAssignment c WHERE c.organization.id = :orgId " +
           "AND c.governorate = :gov AND c.center IS NULL AND c.district IS NULL AND c.active = true")
    Optional<CourierZoneAssignment> findByOrgAndGovOnly(
            @Param("orgId") Long orgId, @Param("gov") Governorate governorate);

    boolean existsByCourierIdAndOrganizationIdAndGovernorateAndDistrictAndActiveTrue(
            Long courierId, Long organizationId, Governorate governorate, String district);

    boolean existsByCourierIdAndOrganizationIdAndGovernorateAndCenterAndAreaAndActiveTrue(
            Long courierId, Long organizationId, Governorate governorate, String center, String area);
}
