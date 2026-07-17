package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Trip;
import com.shipment.shippinggo.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByCode(String code);

    boolean existsByCode(String code);

    List<Trip> findByOriginOrganizationIdOrderByCreatedAtDesc(Long orgId);

    List<Trip> findByDestinationOrganizationIdOrderByCreatedAtDesc(Long orgId);

    // رحلات المنظمة (كمرسل أو مستلم)
    @Query("SELECT t FROM Trip t WHERE t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId ORDER BY t.createdAt DESC")
    List<Trip> findByOrganizationId(@Param("orgId") Long orgId);

    // رحلات بحالة معينة
    @Query("SELECT t FROM Trip t WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) AND t.status = :status ORDER BY t.createdAt DESC")
    List<Trip> findByOrganizationIdAndStatus(@Param("orgId") Long orgId, @Param("status") TripStatus status);

    // رحلات بتاريخ معين
    @Query("SELECT t FROM Trip t WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) AND t.tripDate = :date ORDER BY t.createdAt DESC")
    List<Trip> findByOrganizationIdAndDate(@Param("orgId") Long orgId, @Param("date") LocalDate date);

    // رحلات بيوم عمل معين
    List<Trip> findByBusinessDayIdOrderByCreatedAtDesc(Long businessDayId);

    // رحلات الشاحنة
    List<Trip> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    // رحلات نشطة للشاحنة (غير مكتملة/راجعة)
    @Query("SELECT t FROM Trip t WHERE t.vehicle.id = :vehicleId AND t.status NOT IN ('COMPLETED', 'RETURNED')")
    List<Trip> findActiveByVehicleId(@Param("vehicleId") Long vehicleId);

    // عدد الرحلات المكتملة لشاحنة
    long countByVehicleIdAndStatus(Long vehicleId, TripStatus status);

    // إحصائيات: عدد الرحلات للمنظمة
    @Query("SELECT COUNT(t) FROM Trip t WHERE t.originOrganization.id = :orgId")
    long countByOriginOrganizationId(@Param("orgId") Long orgId);

    // رحلة نشطة (PREPARING) بين نفس المنظمتين وبنفس الشاحنة
    @Query("SELECT t FROM Trip t WHERE t.vehicle.id = :vehicleId AND t.originOrganization.id = :originOrgId AND t.status = 'PREPARING'")
    Optional<Trip> findPreparingTripForVehicle(@Param("vehicleId") Long vehicleId, @Param("originOrgId") Long originOrgId);

    // Dashboard: Active trips count
    @Query("SELECT COUNT(t) FROM Trip t WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) AND t.status NOT IN ('COMPLETED', 'RETURNED', 'CANCELLED')")
    long countActiveTripsForOrg(@Param("orgId") Long orgId);

    // Dashboard: Orders in active trips
    @Query("SELECT COALESCE(SUM(t.orderCount), 0) FROM Trip t WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) AND t.status NOT IN ('COMPLETED', 'RETURNED', 'CANCELLED')")
    long countOrdersInActiveTripsForOrg(@Param("orgId") Long orgId);

    // ===================== Trip Reporting Queries =====================

    /**
     * عدد الرحلات حسب الحالة (صادرة + واردة)
     */
    @Query("SELECT t.status, COUNT(t) FROM Trip t " +
            "WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate) " +
            "GROUP BY t.status")
    List<Object[]> countTripsByStatus(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * إحصائيات الرحلات حسب الشاحنة
     */
    @Query("SELECT t.vehicle.id, t.vehicle.plateNumber, t.vehicle.vehicleType, " +
            "COUNT(t), SUM(t.orderCount), " +
            "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
            "FROM Trip t WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate) " +
            "GROUP BY t.vehicle.id, t.vehicle.plateNumber, t.vehicle.vehicleType")
    List<Object[]> getTripStatsByVehicle(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * الرحلات حسب المنظمة الوجهة (صادرة)
     */
    @Query("SELECT t.destinationOrganization.id, t.destinationOrganization.name, " +
            "COUNT(t), SUM(t.orderCount), " +
            "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
            "FROM Trip t WHERE t.originOrganization.id = :orgId " +
            "AND t.destinationOrganization IS NOT NULL " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate) " +
            "GROUP BY t.destinationOrganization.id, t.destinationOrganization.name")
    List<Object[]> getTripStatsByDestination(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * الرحلات حسب المنظمة المصدر (واردة)
     */
    @Query("SELECT t.originOrganization.id, t.originOrganization.name, " +
            "COUNT(t), SUM(t.orderCount), " +
            "SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
            "FROM Trip t WHERE t.destinationOrganization.id = :orgId " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate) " +
            "GROUP BY t.originOrganization.id, t.originOrganization.name")
    List<Object[]> getTripStatsByOrigin(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * اتجاهات الرحلات يومياً
     */
    @Query("SELECT t.tripDate, COUNT(t) FROM Trip t " +
            "WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate) " +
            "GROUP BY t.tripDate ORDER BY t.tripDate ASC")
    List<Object[]> getTripTrends(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * إجمالي الأوردرات المنقولة
     */
    @Query("SELECT COALESCE(SUM(t.orderCount), 0) FROM Trip t " +
            "WHERE (t.originOrganization.id = :orgId OR t.destinationOrganization.id = :orgId) " +
            "AND (:fromDate IS NULL OR t.tripDate >= :fromDate) " +
            "AND (:toDate IS NULL OR t.tripDate <= :toDate)")
    long countTotalOrdersShipped(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
