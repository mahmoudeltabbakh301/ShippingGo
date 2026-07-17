package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository متخصص في استعلامات التقارير والإحصائيات الزمنية.
 * يشمل: Period stats, Geographic stats, Trend stats, Status stats, Target queries.
 */
@Repository
public interface OrderReportRepository extends JpaRepository<Order, Long> {

    // ===================== Period Stats =====================

    @Query("SELECT COUNT(o) as totalCount, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
            "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedCount, " +
            "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
            "SUM(CASE WHEN o.status = 'DEFERRED' THEN 1 ELSE 0 END) as deferredCount, " +
            "SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as partialCount, " +
            "SUM(CASE WHEN o.status IN ('IN_TRANSIT', 'PICKED_UP', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) as inTransitCount, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalCollected " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    com.shipment.shippinggo.dto.PeriodStatsQueryResult getPeriodStatsByOwnerOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COUNT(o) as totalCount, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
            "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedCount, " +
            "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
            "SUM(CASE WHEN o.status = 'DEFERRED' THEN 1 ELSE 0 END) as deferredCount, " +
            "SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as partialCount, " +
            "SUM(CASE WHEN o.status IN ('IN_TRANSIT', 'PICKED_UP', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) as inTransitCount, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalCollected " +
            "FROM Order o WHERE o.assignedToCourier.id = :courierId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    com.shipment.shippinggo.dto.PeriodStatsQueryResult getPeriodStatsByCourierId(
            @Param("courierId") Long courierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(o.manualCourierCommission), 0) FROM Order o " +
            "WHERE o.assignedToCourier.id = :courierId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    BigDecimal getTotalCommissionByCourierId(
            @Param("courierId") Long courierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COUNT(o) as totalCount, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
            "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedCount, " +
            "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
            "SUM(CASE WHEN o.status = 'DEFERRED' THEN 1 ELSE 0 END) as deferredCount, " +
            "SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as partialCount, " +
            "SUM(CASE WHEN o.status IN ('IN_TRANSIT', 'PICKED_UP', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) as inTransitCount, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalCollected " +
            "FROM Order o WHERE o.ownerOrganization.id = :ownerOrgId AND o.assignedToOrganization.id = :assignedOrgId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    com.shipment.shippinggo.dto.PeriodStatsQueryResult getPeriodStatsByOwnerAndAssignedOrg(
            @Param("ownerOrgId") Long ownerOrgId,
            @Param("assignedOrgId") Long assignedOrgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COALESCE(SUM(o.manualOrgCommission), 0) FROM Order o " +
            "WHERE o.ownerOrganization.id = :ownerOrgId AND o.assignedToOrganization.id = :assignedOrgId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    BigDecimal getTotalCommissionByOwnerAndAssignedOrg(
            @Param("ownerOrgId") Long ownerOrgId,
            @Param("assignedOrgId") Long assignedOrgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Geographic Stats =====================

    @Query("SELECT o.governorate as governorate, COUNT(o) as totalOrders, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
            "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as totalRevenue " +
            "FROM Order o WHERE o.assignedToCourier.id = :courierId " +
            "AND o.governorate IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.governorate")
    List<com.shipment.shippinggo.dto.GeographicStatsQueryResult> getGeographicStatsByCourierId(
            @Param("courierId") Long courierId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT o.governorate as governorate, COUNT(o) as totalOrders, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalRevenue " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND o.governorate IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.governorate")
    List<com.shipment.shippinggo.dto.GeographicStatsQueryResult> getGeographicStatsByOwnerOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Trend Stats =====================

    @Query("SELECT o.originalCreationDate as creationDate, COUNT(o) as totalOrders, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalRevenue " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND o.originalCreationDate IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.originalCreationDate " +
            "ORDER BY o.originalCreationDate ASC")
    List<com.shipment.shippinggo.dto.TrendStatsQueryResult> getTrendStatsByOwnerOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Status Stats =====================

    @Query("SELECT o.status as status, COUNT(o) as count, " +
            "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as revenue " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId GROUP BY o.status")
    List<com.shipment.shippinggo.dto.StatusStatsQueryResult> getStatusStatsByOwnerOrg(
            @Param("orgId") Long orgId);

    // ===================== In/Out Stats =====================

    @Query("SELECT COUNT(o) as totalOrders, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN o.amount " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalCollected " +
            "FROM Order o WHERE o.assignedToOrganization.id = :orgId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    com.shipment.shippinggo.dto.OrganizationInOutStats getIncomingStatsForOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query("SELECT COUNT(o) as totalOrders, " +
            "COALESCE(SUM(CASE " +
            "  WHEN o.status = 'DELIVERED' THEN o.amount " +
            "  WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount " +
            "  ELSE 0 END), 0) as totalCollected " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate)")
    com.shipment.shippinggo.dto.OrganizationInOutStats getOutgoingStatsForOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Target Setting Queries =====================

    @Query("SELECT COALESCE(SUM(CASE " +
            "WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "WHEN o.status = 'PARTIAL_DELIVERY' THEN COALESCE(o.partialDeliveryAmount, 0) " +
            "ELSE 0 END), 0) " +
            "FROM Order o WHERE o.assignedToCourier.id = :courierId " +
            "AND o.courierAssignmentDate >= :startDate AND o.courierAssignmentDate < :endDate " +
            "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
    BigDecimal sumDeliveredAmountByCourierAndDateRange(
            @Param("courierId") Long courierId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.assignedToCourier.id = :courierId " +
            "AND o.courierAssignmentDate >= :startDate AND o.courierAssignmentDate < :endDate " +
            "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
    List<Order> findDeliveredOrdersByCourierAndDateRange(
            @Param("courierId") Long courierId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(CASE " +
            "WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
            "WHEN o.status = 'PARTIAL_DELIVERY' THEN COALESCE(o.partialDeliveryAmount, 0) " +
            "ELSE 0 END), 0) " +
            "FROM Order o WHERE o.assignedToOrganization.id = :assignedOrgId " +
            "AND o.assignmentDate >= CAST(:startDate AS java.time.LocalDate) AND o.assignmentDate <= CAST(:endDate AS java.time.LocalDate) " +
            "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
    BigDecimal sumDeliveredAmountByAssignedOrgAndDateRange(
            @Param("assignedOrgId") Long assignedOrgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.assignedToOrganization.id = :assignedOrgId " +
            "AND o.assignmentDate >= CAST(:startDate AS java.time.LocalDate) AND o.assignmentDate <= CAST(:endDate AS java.time.LocalDate) " +
            "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
    List<Order> findDeliveredOrdersByAssignedOrgAndDateRange(
            @Param("assignedOrgId") Long assignedOrgId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ===================== Total Commissions =====================

    /**
     * إجمالي العمولات (المندوب + المنظمة) لأوردرات منظمة في فترة محددة
     * يستخدم AccountTransaction كمصدر حقيقي للعمولات المسجلة
     */
    @Query(value = "SELECT COALESCE(SUM(at.amount), 0) FROM account_transactions at " +
            "JOIN orders o ON at.order_id = o.id " +
            "WHERE o.owner_organization_id = :orgId " +
            "AND (:fromDate IS NULL OR o.original_creation_date >= :fromDate) " +
            "AND (:toDate IS NULL OR o.original_creation_date <= :toDate)", nativeQuery = true)
    BigDecimal getTotalCommissionsByOwnerOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Delivery Time Queries =====================

    /**
     * متوسط وقت التوصيل (بالدقائق) لمنظمة - من إسناد المندوب حتى التسليم
     * يعتمد على courierAssignmentDate (بداية) و changedAt في StatusHistory (نهاية)
     */
    @Query(value = "SELECT AVG(TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at)) as avgMinutes, " +
            "MIN(TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at)) as minMinutes, " +
            "MAX(TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at)) as maxMinutes, " +
            "COUNT(*) as totalDelivered " +
            "FROM orders o JOIN order_status_history osh ON o.id = osh.order_id " +
            "WHERE o.owner_organization_id = :orgId " +
            "AND osh.new_status = 'DELIVERED' AND o.courier_assignment_date IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.original_creation_date >= :fromDate) " +
            "AND (:toDate IS NULL OR o.original_creation_date <= :toDate)", nativeQuery = true)
    List<Object[]> getDeliveryTimeStatsByOrg(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * متوسط وقت التوصيل حسب المندوب
     */
    @Query(value = "SELECT o.assigned_to_courier_id as courierId, " +
            "AVG(TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at)) as avgMinutes, " +
            "COUNT(*) as totalDelivered " +
            "FROM orders o JOIN order_status_history osh ON o.id = osh.order_id " +
            "WHERE o.owner_organization_id = :orgId " +
            "AND osh.new_status = 'DELIVERED' AND o.courier_assignment_date IS NOT NULL " +
            "AND o.assigned_to_courier_id IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.original_creation_date >= :fromDate) " +
            "AND (:toDate IS NULL OR o.original_creation_date <= :toDate) " +
            "GROUP BY o.assigned_to_courier_id ORDER BY avgMinutes ASC", nativeQuery = true)
    List<Object[]> getDeliveryTimeByCourier(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * متوسط وقت التوصيل حسب المحافظة
     */
    @Query(value = "SELECT o.governorate as gov, " +
            "AVG(TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at)) as avgMinutes, " +
            "COUNT(*) as totalDelivered " +
            "FROM orders o JOIN order_status_history osh ON o.id = osh.order_id " +
            "WHERE o.owner_organization_id = :orgId " +
            "AND osh.new_status = 'DELIVERED' AND o.courier_assignment_date IS NOT NULL " +
            "AND o.governorate IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.original_creation_date >= :fromDate) " +
            "AND (:toDate IS NULL OR o.original_creation_date <= :toDate) " +
            "GROUP BY o.governorate ORDER BY avgMinutes ASC", nativeQuery = true)
    List<Object[]> getDeliveryTimeByGovernorate(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * توزيع الأوردرات حسب فئات وقت التوصيل (بالدقائق)
     */
    @Query(value = "SELECT " +
            "SUM(CASE WHEN TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) <= 120 THEN 1 ELSE 0 END) as within2h, " +
            "SUM(CASE WHEN TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) > 120 AND TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) <= 240 THEN 1 ELSE 0 END) as within4h, " +
            "SUM(CASE WHEN TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) > 240 AND TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) <= 480 THEN 1 ELSE 0 END) as within8h, " +
            "SUM(CASE WHEN TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) > 480 AND TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) <= 1440 THEN 1 ELSE 0 END) as within24h, " +
            "SUM(CASE WHEN TIMESTAMPDIFF(MINUTE, o.courier_assignment_date, osh.changed_at) > 1440 THEN 1 ELSE 0 END) as moreThan24h " +
            "FROM orders o JOIN order_status_history osh ON o.id = osh.order_id " +
            "WHERE o.owner_organization_id = :orgId " +
            "AND osh.new_status = 'DELIVERED' AND o.courier_assignment_date IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.original_creation_date >= :fromDate) " +
            "AND (:toDate IS NULL OR o.original_creation_date <= :toDate)", nativeQuery = true)
    List<Object[]> getDeliveryTimeDistribution(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ===================== Returns/Rejection Queries =====================

    /**
     * عدد المرتجعات حسب السبب
     */
    @Query("SELECT o.rejectionReason, COUNT(o) FROM Order o " +
            "WHERE o.ownerOrganization.id = :orgId " +
            "AND o.status IN ('REFUSED', 'CANCELLED', 'DEFERRED') " +
            "AND o.rejectionReason IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.rejectionReason ORDER BY COUNT(o) DESC")
    List<Object[]> getReturnsByReason(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * المرتجعات حسب المندوب
     */
    @Query("SELECT o.assignedToCourier.id, COUNT(o), " +
            "SUM(CASE WHEN o.status IN ('REFUSED', 'CANCELLED', 'DEFERRED') THEN 1 ELSE 0 END) " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND o.assignedToCourier IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.assignedToCourier.id")
    List<Object[]> getReturnsByCourier(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * المرتجعات حسب المحافظة
     */
    @Query("SELECT o.governorate, COUNT(o), " +
            "SUM(CASE WHEN o.status IN ('REFUSED', 'CANCELLED', 'DEFERRED') THEN 1 ELSE 0 END) " +
            "FROM Order o WHERE o.ownerOrganization.id = :orgId " +
            "AND o.governorate IS NOT NULL " +
            "AND (:fromDate IS NULL OR o.originalCreationDate >= :fromDate) " +
            "AND (:toDate IS NULL OR o.originalCreationDate <= :toDate) " +
            "GROUP BY o.governorate")
    List<Object[]> getReturnsByGovernorate(
            @Param("orgId") Long orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
