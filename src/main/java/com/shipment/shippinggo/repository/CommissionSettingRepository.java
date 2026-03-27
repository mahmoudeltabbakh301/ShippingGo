package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.CommissionSetting;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommissionSettingRepository extends JpaRepository<CommissionSetting, Long> {

        // البحث عن إعداد عمولة افتراضي (بدون محافظة) بين منظمتين
        Optional<CommissionSetting> findBySourceOrganizationAndTargetOrganizationAndGovernorateIsNull(
                        Organization sourceOrganization, Organization targetOrganization);

        // البحث عن إعداد عمولة مخصص لمحافظة معينة بين منظمتين
        Optional<CommissionSetting> findBySourceOrganizationAndTargetOrganizationAndGovernorate(
                        Organization sourceOrganization, Organization targetOrganization,
                        com.shipment.shippinggo.enums.Governorate governorate);

        // البحث عن إعداد عمولة لمندوب في منظمة معينة
        Optional<CommissionSetting> findBySourceOrganizationAndCourier(
                        Organization sourceOrganization, User courier);

        // جميع إعدادات العمولات لمنظمة معينة
        List<CommissionSetting> findBySourceOrganization(Organization organization);

        // جميع إعدادات العمولات للمناديب
        List<CommissionSetting> findBySourceOrganizationAndCourierIsNotNull(Organization organization);

        // جميع إعدادات العمولات للمنظمات
        List<CommissionSetting> findBySourceOrganizationAndTargetOrganizationIsNotNull(Organization organization);
}
