package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.TripOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripOrderRepository extends JpaRepository<TripOrder, Long> {

    List<TripOrder> findByTripIdOrderByLoadedAtDesc(Long tripId);

    Optional<TripOrder> findByTripIdAndOrderId(Long tripId, Long orderId);

    boolean existsByTripIdAndOrderId(Long tripId, Long orderId);

    // هل الأوردر محمل في أي رحلة نشطة؟
    @Query("SELECT CASE WHEN COUNT(to2) > 0 THEN true ELSE false END FROM TripOrder to2 " +
           "JOIN to2.trip t WHERE to2.order.id = :orderId AND t.status NOT IN ('COMPLETED', 'RETURNED', 'CANCELLED')")
    boolean isOrderInActiveTrip(@Param("orderId") Long orderId);

    // جلب الرحلة النشطة التي يتبعها الأوردر
    @Query("SELECT to2 FROM TripOrder to2 JOIN to2.trip t WHERE to2.order.id = :orderId AND t.status NOT IN ('COMPLETED', 'RETURNED', 'CANCELLED')")
    Optional<TripOrder> findActiveTripOrderByOrderId(@Param("orderId") Long orderId);

    long countByTripId(Long tripId);

    // عدد الأوردرات المنقولة بشاحنة معينة (لأغراض التقارير)
    @Query("SELECT COUNT(to2) FROM TripOrder to2 WHERE to2.trip.vehicle.id = :vehicleId")
    long countOrdersByVehicleId(@Param("vehicleId") Long vehicleId);

    void deleteByTripIdAndOrderId(Long tripId, Long orderId);
}
