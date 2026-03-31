package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.AccountBusinessDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountBusinessDayRepository extends JpaRepository<AccountBusinessDay, Long> {

    // جلب يوم حسابات حسب يوم العمل
    Optional<AccountBusinessDay> findByBusinessDayId(Long businessDayId);

    // جلب يوم حسابات حسب يوم العمل والمنظمة
    Optional<AccountBusinessDay> findByBusinessDayIdAndOrganizationId(Long businessDayId, Long organizationId);

    // جلب جميع أيام الحسابات لمنظمة مرتبة تنازلياً
    List<AccountBusinessDay> findByOrganizationIdOrderByBusinessDayDateDesc(Long organizationId);

    // جلب أيام الحسابات بدون أيام العهدة
    List<AccountBusinessDay> findByOrganizationIdAndBusinessDayIsCustodyFalseOrderByBusinessDayDateDesc(
            Long organizationId);

    // جلب أيام الحسابات مُقسمة لصفحات
    org.springframework.data.domain.Page<AccountBusinessDay> findByOrganizationIdAndBusinessDayIsCustodyFalseOrderByBusinessDayDateDesc(
            Long organizationId, org.springframework.data.domain.Pageable pageable);

    // جلب يوم حسابات حسب التاريخ والمنظمة
    Optional<AccountBusinessDay> findByOrganizationIdAndBusinessDayDate(Long organizationId, LocalDate date);

    // جلب أيام الحسابات النشطة فقط
    List<AccountBusinessDay> findByOrganizationIdAndActiveOrderByBusinessDayDateDesc(Long organizationId,
            boolean active);

    // حذف جميع أيام الحسابات ليوم عمل محدد
    void deleteByBusinessDayId(Long businessDayId);
}
