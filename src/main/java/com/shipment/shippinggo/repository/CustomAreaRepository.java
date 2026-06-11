package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.CustomArea;
import com.shipment.shippinggo.enums.Governorate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomAreaRepository extends JpaRepository<CustomArea, Long> {

    /**
     * الحصول على المناطق المخصصة لمنظمة في مركز معين
     */
    List<CustomArea> findByOrganizationIdAndGovernorateAndCenter(Long organizationId, Governorate governorate, String center);

    /**
     * الحصول على كل المناطق المخصصة لمنظمة
     */
    List<CustomArea> findByOrganizationIdOrderByGovernorateAscCenterAscNameAsc(Long organizationId);

    /**
     * التحقق من وجود منطقة مخصصة بنفس الاسم
     */
    boolean existsByOrganizationIdAndGovernorateAndCenterAndName(Long organizationId, Governorate governorate, String center, String name);

    /**
     * حذف منطقة مخصصة بالـ ID والمنظمة (للأمان)
     */
    void deleteByIdAndOrganizationId(Long id, Long organizationId);
}
