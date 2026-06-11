package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.TargetSetting;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TargetSettingRepository extends JpaRepository<TargetSetting, Long> {

    // جميع إعدادات التارجت لمنظمة معينة
    List<TargetSetting> findBySourceOrganization(Organization organization);

    // جميع إعدادات التارجت النشطة لمنظمة معينة
    List<TargetSetting> findBySourceOrganizationAndActiveTrue(Organization organization);

    // تارجت مندوب محدد في منظمة معينة
    Optional<TargetSetting> findBySourceOrganizationAndCourier(Organization sourceOrganization, User courier);

    // تارجت منظمة مستهدفة محددة في منظمة مصدر معينة
    Optional<TargetSetting> findBySourceOrganizationAndTargetOrganization(
            Organization sourceOrganization, Organization targetOrganization);

    // تارجت مندوب نشط
    Optional<TargetSetting> findBySourceOrganizationAndCourierAndActiveTrue(
            Organization sourceOrganization, User courier);

    // تارجت منظمة نشط
    Optional<TargetSetting> findBySourceOrganizationAndTargetOrganizationAndActiveTrue(
            Organization sourceOrganization, Organization targetOrganization);
}
