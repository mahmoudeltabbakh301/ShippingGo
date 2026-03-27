package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.CourierDayLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourierDayLogRepository extends JpaRepository<CourierDayLog, Long> {

    // جلب سجل يوم محدد للمندوب
    Optional<CourierDayLog> findByCourierIdAndDate(Long courierId, LocalDate date);

    // جلب جميع سجلات الأيام للمندوب مرتبة تنازلياً
    List<CourierDayLog> findByCourierIdOrderByDateDesc(Long courierId);

    // جلب سجلات فترة محددة
    List<CourierDayLog> findByCourierIdAndDateBetweenOrderByDateDesc(
            Long courierId, LocalDate startDate, LocalDate endDate);
}
