package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        // Orders by Code
        Optional<Order> findByCode(String code);

        boolean existsByCode(String code);

        // Orders by Creator
        List<Order> findByCreatedByIdOrderByCreatedAtDesc(Long createdById);

        // Orders by BusinessDay
        List<Order> findByBusinessDay(BusinessDay businessDay);

        List<Order> findByBusinessDayId(Long businessDayId);

        // Orders by Owner Organization
        List<Order> findByOwnerOrganization(Organization organization);

        List<Order> findByOwnerOrganizationId(Long organizationId);

        List<Order> findByOwnerOrganizationIdAndOriginalCreationDateBetween(Long organizationId, LocalDate fromDate,
                        LocalDate toDate);

        long countByOwnerOrganizationIdAndStatusAndOriginalCreationDateBetween(Long organizationId, OrderStatus status,
                        LocalDate fromDate, LocalDate toDate);

        @Query("SELECT COUNT(o) as totalCount, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
                        "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedCount, " +
                        "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
                        "SUM(CASE WHEN o.status = 'DEFERRED' THEN 1 ELSE 0 END) as deferredCount, " +
                        "SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as partialCount, " +
                        "SUM(CASE WHEN o.status = 'IN_TRANSIT' THEN 1 ELSE 0 END) as inTransitCount, " +
                        "COALESCE(SUM(CASE " +
                        "  WHEN o.status = 'DELIVERED' THEN o.amount " +
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
                        "SUM(CASE WHEN o.status = 'IN_TRANSIT' THEN 1 ELSE 0 END) as inTransitCount, " +
                        "COALESCE(SUM(CASE " +
                        "  WHEN o.status = 'DELIVERED' THEN o.amount " +
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
        java.math.BigDecimal getTotalCommissionByCourierId(
               @Param("courierId") Long courierId,
               @Param("fromDate") LocalDate fromDate,
               @Param("toDate") LocalDate toDate);

        @Query("SELECT o.governorate as governorate, COUNT(o) as totalOrders, " +
               "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
               "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as totalRevenue " +
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
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, "
                        +
                        "COALESCE(SUM(CASE " +
                        "  WHEN o.status = 'DELIVERED' THEN o.amount " +
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

        @Query("SELECT o.originalCreationDate as creationDate, COUNT(o) as totalOrders, " +
                        "COALESCE(SUM(CASE " +
                        "  WHEN o.status = 'DELIVERED' THEN o.amount " +
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

        List<Order> findByOwnerOrganizationIdAndStatusAndInvoiceIsNull(Long organizationId, OrderStatus status);

        List<Order> findByOwnerOrganizationIdOrderByCreatedAtDesc(Long organizationId);

        List<Order> findByInvoiceId(Long invoiceId);

        // Orders assigned to Organization
        List<Order> findByAssignedToOrganization(Organization organization);

        List<Order> findByAssignedToOrganizationId(Long organizationId);

        List<Order> findByAssignedToOrganizationIdAndOriginalCreationDateBetween(Long organizationId,
                        LocalDate fromDate, LocalDate toDate);

        // Orders by Creator Organization (for Stores)
        List<Order> findByCreatorOrganizationIdOrderByCreatedAtDesc(Long organizationId);

        // Orders assigned to Courier
        List<Order> findByAssignedToCourier(User courier);

        List<Order> findByAssignedToCourierId(Long courierId);

        List<Order> findByAssignedToCourierIdAndOriginalCreationDateBetween(Long courierId, LocalDate fromDate,
                        LocalDate toDate);

        List<Order> findByAssignedToCourierIdAndStatus(Long courierId, OrderStatus status);

        // Orders by Status
        List<Order> findByOwnerOrganizationIdAndStatus(Long organizationId, OrderStatus status);

        List<Order> findByAssignedToCourierIdOrderByCreatedAtDesc(Long courierId);

        // Count queries
        long countByOwnerOrganizationIdAndStatus(Long organizationId, OrderStatus status);

        long countByAssignedToCourierIdAndStatus(Long courierId, OrderStatus status);

        // Date-based queries
        @Query("SELECT o FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.originalCreationDate = :date")
        List<Order> findByOwnerOrganizationIdAndOriginalCreationDate(@Param("orgId") Long orgId,
                        @Param("date") LocalDate date);

        @Query("SELECT o FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date")
        List<Order> findByAssignedToOrganizationIdAndAssignmentDate(@Param("orgId") Long orgId,
                        @Param("date") LocalDate date);

        @Query("SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.ownerOrganization " +
                        "LEFT JOIN FETCH o.assignedToOrganization " +
                        "LEFT JOIN FETCH o.assignedToCourier " +
                        "LEFT JOIN FETCH o.businessDay " +
                        "WHERE " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:code IS NULL OR LOWER(o.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
                        "(" +
                        "   (:courierId IS NULL) OR " +
                        "   (:courierId = -1 AND o.assignedToCourier IS NULL) OR " +
                        "   (o.assignedToCourier.id = :courierId)" +
                        ") AND " +
                        "(" +
                        "   (:officeId IS NULL) OR " +
                        "   (:officeId = -1 AND o.assignedToOrganization IS NULL) OR " +
                        "   (o.assignedToOrganization.id = :officeId) OR " +
                        "   (o.ownerOrganization.id = :officeId AND o.assignedToOrganization.id = :orgId)" +
                        ") AND " +
                        "(:governorate IS NULL OR o.governorate = :governorate) AND " +
                        "(o.ownerOrganization.id = :orgId OR o.assignedToOrganization.id = :orgId OR o.creatorOrganization.id = :orgId)")
        List<Order> findOrdersWithFilters(
                        @Param("orgId") Long orgId,
                        @Param("code") String code,
                        @Param("courierId") Long courierId,
                        @Param("officeId") Long officeId,
                        @Param("status") OrderStatus status,
                        @Param("governorate") Governorate governorate);

        // For accounts - orders assigned to organization
        long countByAssignedToOrganizationId(Long organizationId);

        long countByAssignedToOrganizationIdAndStatus(Long organizationId, OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId")
        java.math.BigDecimal sumAmountByAssignedToOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.status = :status")
        java.math.BigDecimal sumAmountByAssignedToOrganizationIdAndStatus(@Param("orgId") Long orgId,
                        @Param("status") OrderStatus status);

        // For accounts - orders by courier
        long countByAssignedToCourierId(Long courierId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId")
        java.math.BigDecimal sumAmountByAssignedToCourierId(@Param("courierId") Long courierId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.status = :status")
        java.math.BigDecimal sumAmountByAssignedToCourierIdAndStatus(@Param("courierId") Long courierId,
                        @Param("status") OrderStatus status);

        // Courier orders by date (for daily reset)
        @Query("SELECT o FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay ORDER BY o.courierAssignmentDate DESC")
        List<Order> findByAssignedToCourierIdAndCourierAssignmentDate(
                        @Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        // Count courier orders by date and status
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay AND o.status = :status")
        long countByAssignedToCourierIdAndCourierAssignmentDateAndStatus(
                        @Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay,
                        @Param("status") OrderStatus status);

        // Count all courier orders by date
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay")
        long countByAssignedToCourierIdAndCourierAssignmentDate(
                        @Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        // ===================== Business Day filtered queries for Accounts
        // =====================

        // Organization counts by businessDay
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        long countByAssignedToOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = :status")
        long countByAssignedToOrganizationIdAndBusinessDayIdAndStatus(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        // Organization sums by businessDay
        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        java.math.BigDecimal sumAmountByAssignedToOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = :status")
        java.math.BigDecimal sumAmountByAssignedToOrganizationIdAndBusinessDayIdAndStatus(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        // Courier counts by businessDay
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId")
        long countByAssignedToCourierIdAndBusinessDayId(@Param("courierId") Long courierId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId AND o.status = :status")
        long countByAssignedToCourierIdAndBusinessDayIdAndStatus(@Param("courierId") Long courierId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        // Courier sums by businessDay
        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId")
        java.math.BigDecimal sumAmountByAssignedToCourierIdAndBusinessDayId(@Param("courierId") Long courierId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId AND o.status = :status")
        java.math.BigDecimal sumAmountByAssignedToCourierIdAndBusinessDayIdAndStatus(@Param("courierId") Long courierId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        // Find orders by assignee and business day
        @Query("SELECT o FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId ORDER BY o.createdAt DESC")
        List<Order> findByAssignedToOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT o FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId ORDER BY o.createdAt DESC")
        List<Order> findByAssignedToCourierIdAndBusinessDayId(@Param("courierId") Long courierId,
                        @Param("businessDayId") Long businessDayId);

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
                        
        @Query("SELECT o.status as status, COUNT(o) as count, " +
               "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as revenue " +
               "FROM Order o WHERE o.ownerOrganization.id = :orgId GROUP BY o.status")
        List<com.shipment.shippinggo.dto.StatusStatsQueryResult> getStatusStatsByOwnerOrg(
               @Param("orgId") Long orgId);

        @Query("SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.ownerOrganization " +
                        "LEFT JOIN FETCH o.assignedToOrganization " +
                        "LEFT JOIN FETCH o.assignedToCourier " +
                        "LEFT JOIN FETCH o.businessDay " +
                        "WHERE " +
                        "(o.businessDay.id = :businessDayId OR EXISTS (" +
                        "   SELECT 1 FROM OrderAssignment oa WHERE oa.order = o AND oa.businessDay.id = :businessDayId AND oa.accepted = true"
                        +
                        ")) AND " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:search IS NULL OR (" +
                        "   LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "   LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "   LOWER(o.recipientAddress) LIKE LOWER(CONCAT('%', :search, '%'))" +
                        ")) AND " +
                        "(:code IS NULL OR LOWER(o.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
                        "(" +
                        "   (:courierId IS NULL) OR " +
                        "   (:courierId = -1 AND o.assignedToCourier IS NULL) OR " +
                        "   (o.assignedToCourier.id = :courierId)" +
                        ") AND " +
                        "(" +
                        "   (:officeId IS NULL) OR " +
                        "   (:officeId = -1 AND o.assignedToOrganization IS NULL) OR " +
                        "   (o.assignedToOrganization.id = :officeId)" +
                        ") AND " +
                        "(:governorate IS NULL OR o.governorate = :governorate)")
        List<Order> findOrdersByBusinessDayWithFilters(
                        @Param("businessDayId") Long businessDayId,
                        @Param("search") String search,
                        @Param("code") String code,
                        @Param("courierId") Long courierId,
                        @Param("officeId") Long officeId,
                        @Param("status") OrderStatus status,
                        @Param("governorate") Governorate governorate);

        @Query("SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.ownerOrganization " +
                        "LEFT JOIN FETCH o.assignedToOrganization " +
                        "LEFT JOIN FETCH o.assignedToCourier " +
                        "LEFT JOIN FETCH o.businessDay " +
                        "WHERE " +
                        "((o.businessDay.organization.id = :orgId AND o.businessDay.isCustody = true) OR " +
                        "EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.isCustody = true)) AND " +
                        "(:search IS NULL OR LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR o.recipientAddress LIKE CONCAT('%', :search, '%')) AND " +
                        "(:code IS NULL OR o.code LIKE CONCAT('%', :code, '%')) AND " +
                        "(:courierId IS NULL OR (o.assignedToCourier.id = :courierId)) AND " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:governorate IS NULL OR o.governorate = :governorate) AND " +
                        "(:incomingFromId IS NULL OR EXISTS (" +
                        "   SELECT 1 FROM OrderAssignment oa2 WHERE oa2.order = o AND oa2.assigneeOrganization.id = :orgId AND oa2.assignerOrganization.id = :incomingFromId" +
                        ")) AND " +
                        "(:outgoingToId IS NULL OR EXISTS (" +
                        "   SELECT 1 FROM OrderAssignment oa3 WHERE oa3.order = o AND oa3.assignerOrganization.id = :orgId AND oa3.assigneeOrganization.id = :outgoingToId" +
                        "))")
        List<Order> findCustodyOrdersWithFilters(
                        @Param("orgId") Long orgId,
                        @Param("search") String search,
                        @Param("code") String code,
                        @Param("courierId") Long courierId,
                        @Param("status") OrderStatus status,
                        @Param("governorate") com.shipment.shippinggo.enums.Governorate governorate,
                        @Param("incomingFromId") Long incomingFromId,
                        @Param("outgoingToId") Long outgoingToId);

        // Owner Organization queries by businessDay (for Offices viewing Companies)
        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        long countByOwnerOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = :status")
        long countByOwnerOrganizationIdAndBusinessDayIdAndStatus(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        java.math.BigDecimal sumAmountByOwnerOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = :status")
        java.math.BigDecimal sumAmountByOwnerOrganizationIdAndBusinessDayIdAndStatus(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        @Query("SELECT o FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId ORDER BY o.createdAt DESC")
        List<Order> findByOwnerOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        // ===================== Assignment Date Queries (for Office View - Decoupled)
        // =====================

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date")
        long countByAssignedToOrganizationIdAndAssignmentDate(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date AND o.status = :status")
        long countByAssignedToOrganizationIdAndAssignmentDateAndStatus(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date")
        java.math.BigDecimal sumAmountByAssignedToOrganizationIdAndAssignmentDate(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date AND o.status = :status")
        java.math.BigDecimal sumAmountByAssignedToOrganizationIdAndAssignmentDateAndStatus(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date")
        java.math.BigDecimal sumRejectionPaymentByAssignedToOrganizationIdAndAssignmentDate(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.assignmentDate = :date AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByAssignedToOrganizationIdAndAssignmentDate(@Param("orgId") Long orgId,
                        @Param("date") java.time.LocalDate date);

        // ===================== Assignment Date Queries (for Office views)
        // =====================
        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date")
        long countByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId, @Param("assignedId") Long assignedId,
                        @Param("date") java.time.LocalDate date);

        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date AND o.status = :status")
        long countByOwnerOrgAndAssignedOrgAndDateAndStatus(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date,
                        @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date")
        java.math.BigDecimal sumByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date AND o.status = :status")
        java.math.BigDecimal sumByOwnerOrgAndAssignedOrgAndDateAndStatus(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date,
                        @Param("status") OrderStatus status);

        @Query("SELECT o FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date ORDER BY o.createdAt DESC")
        List<Order> findByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date);

        // ===================== Courier Assignment Date Queries =====================
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay")
        long countByAssignedToCourierIdAndAssignmentDate(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay AND o.status = :status")
        long countByAssignedToCourierIdAndAssignmentDateAndStatus(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay")
        java.math.BigDecimal sumByAssignedToCourierIdAndAssignmentDate(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay AND o.status = :status")
        java.math.BigDecimal sumByAssignedToCourierIdAndAssignmentDateAndStatus(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay, @Param("status") OrderStatus status);

        @Query("SELECT o FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay ORDER BY o.createdAt DESC")
        List<Order> findByAssignedToCourierIdAndAssignmentDate(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        // ===================== All Time Owner queries for Accounts
        // =====================
        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId")
        long countByOwnerOrganizationId(@Param("orgId") Long orgId);

        // countByOwnerOrganizationIdAndStatus is already defined at line 49

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId")
        java.math.BigDecimal sumAmountByOwnerOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.status = :status")
        java.math.BigDecimal sumAmountByOwnerOrganizationIdAndStatus(@Param("orgId") Long orgId,
                        @Param("status") OrderStatus status);

        // ===================== Rejection Logic Queries =====================

        // --- Assigned to Organization ---

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByAssignedToOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByAssignedToOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId")
        java.math.BigDecimal sumRejectionPaymentByAssignedToOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.assignedToOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        java.math.BigDecimal sumRejectionPaymentByAssignedToOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        // --- Assigned to Courier ---

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByAssignedToCourierId(@Param("courierId") Long courierId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByAssignedToCourierIdAndAssignmentDate(@Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId")
        java.math.BigDecimal sumRejectionPaymentByAssignedToCourierId(@Param("courierId") Long courierId);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay")
        java.math.BigDecimal sumRejectionPaymentByAssignedToCourierIdAndAssignmentDate(
                        @Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        // --- Owner Organization (For Office View) ---

        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByOwnerOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByOwnerOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId")
        java.math.BigDecimal sumRejectionPaymentByOwnerOrganizationId(@Param("orgId") Long orgId);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        java.math.BigDecimal sumRejectionPaymentByOwnerOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        // --- Owner Org + Assigned Org + Date (Office viewing Office) ---

        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date")
        java.math.BigDecimal sumRejectionPaymentByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date);

        // ===================== Global Owner + Assigned Queries (for Strict Company ->
        // Office View) =====================

        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId")
        long countByOwnerOrganizationIdAndAssignedToOrganizationId(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId);

        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND o.status = :status")
        long countByOwnerOrganizationIdAndAssignedToOrganizationIdAndStatus(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE o.amount END), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId")
        java.math.BigDecimal sumAmountByOwnerOrganizationIdAndAssignedToOrganizationId(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId);

        @Query("SELECT COALESCE(SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE o.amount END), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND o.status = :status")
        java.math.BigDecimal sumAmountByOwnerOrganizationIdAndAssignedToOrganizationIdAndStatus(
                        @Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(o.rejectionPayment), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId")
        java.math.BigDecimal sumRejectionPaymentByOwnerOrganizationIdAndAssignedToOrganizationId(
                        @Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId);

        @Query("SELECT COUNT(o) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND o.status = 'REFUSED' AND (o.rejectionPayment IS NULL OR o.rejectionPayment = 0)")
        long countUnpaidRefusedByOwnerOrganizationIdAndAssignedToOrganizationId(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId);

        @Query("SELECT COUNT(o) as totalCount, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredCount, "
                        +
                        "SUM(CASE WHEN o.status = 'REFUSED' OR o.status = 'CANCELLED' OR o.status = 'DEFERRED' THEN 1 ELSE 0 END) as refusedCount, "
                        +
                        "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
                        "SUM(CASE WHEN o.status = 'IN_TRANSIT' THEN 1 ELSE 0 END) as inTransitCount, " +
                        "COALESCE(SUM(CASE " +
                        "  WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN " +
                        "    COALESCE(o.collectedAmount, CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE o.amount END) "
                        +
                        "  WHEN o.status = 'REFUSED' THEN COALESCE(o.rejectionPayment, 0) " +
                        "  ELSE 0 END), 0) as deliveredAmount " +
                        "FROM Order o WHERE o.assignedToCourier.id = :courierId " +
                        "AND o.courierAssignmentDate >= :startDate " +
                        "AND o.courierAssignmentDate < :endDate")
        com.shipment.shippinggo.dto.CourierStatsQueryResult getCourierTodayStats(
                        @Param("courierId") Long courierId,
                        @Param("startDate") java.time.LocalDateTime startDate,
                        @Param("endDate") java.time.LocalDateTime endDate);

        @Query("SELECT o FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId ORDER BY o.createdAt DESC")
        List<Order> findByOwnerOrganizationIdAndAssignedToOrganizationId(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId);

        @Query("SELECT COUNT(o) as totalOrders, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, "
                        +
                        "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedOrders, " +
                        "COALESCE(SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE o.amount END), 0) as totalAmount, "
                        +
                        "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as deliveredAmount "
                        +
                        "FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId")
        com.shipment.shippinggo.dto.AccountSummaryQueryResult getAccountSummaryByOwnerAndAssigned(
                        @Param("ownerId") Long ownerId, @Param("assignedId") Long assignedId);

        @Query("SELECT COUNT(o) as totalOrders, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, "
                        +
                        "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedOrders, " +
                        "COALESCE(SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE o.amount END), 0) as totalAmount, "
                        +
                        "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as deliveredAmount "
                        +
                        "FROM Order o WHERE o.ownerOrganization.id = :ownerId")
        com.shipment.shippinggo.dto.AccountSummaryQueryResult getAccountSummaryByOwner(@Param("ownerId") Long ownerId);

        // ===================== Partial Delivery Amount Queries =====================

        // Sum partialDeliveryAmount for Owner+Assigned+Date (for directional summaries)
        @Query("SELECT COALESCE(SUM(o.partialDeliveryAmount), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND oa.assignmentDate = :date AND o.status = 'PARTIAL_DELIVERY'")
        java.math.BigDecimal sumPartialDeliveryAmountByOwnerOrgAndAssignedOrgAndDate(@Param("ownerId") Long ownerId,
                        @Param("assignedId") Long assignedId, @Param("date") java.time.LocalDate date);

        // Sum partialDeliveryAmount for Owner+Assigned (global)
        @Query("SELECT COALESCE(SUM(o.partialDeliveryAmount), 0) FROM Order o JOIN OrderAssignment oa ON oa.order = o WHERE oa.assignerOrganization.id = :ownerId AND oa.assigneeOrganization.id = :assignedId AND o.status = 'PARTIAL_DELIVERY'")
        java.math.BigDecimal sumPartialDeliveryAmountByOwnerOrganizationIdAndAssignedToOrganizationId(
                        @Param("ownerId") Long ownerId, @Param("assignedId") Long assignedId);

        // Sum partialDeliveryAmount for Courier by assignment date
        @Query("SELECT COALESCE(SUM(o.partialDeliveryAmount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.courierAssignmentDate >= :startOfDay AND o.courierAssignmentDate < :startOfNextDay AND o.status = 'PARTIAL_DELIVERY'")
        java.math.BigDecimal sumPartialDeliveryAmountByAssignedToCourierIdAndAssignmentDate(
                        @Param("courierId") Long courierId,
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("startOfNextDay") LocalDateTime startOfNextDay);

        // Sum partialDeliveryAmount for Courier (all time)
        @Query("SELECT COALESCE(SUM(o.partialDeliveryAmount), 0) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.status = 'PARTIAL_DELIVERY'")
        java.math.BigDecimal sumPartialDeliveryAmountByAssignedToCourierId(@Param("courierId") Long courierId);

        // Sum partialDeliveryAmount for Owner by businessDay
        @Query("SELECT COALESCE(SUM(o.partialDeliveryAmount), 0) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = 'PARTIAL_DELIVERY'")
        java.math.BigDecimal sumPartialDeliveryAmountByOwnerOrganizationIdAndBusinessDayId(
                        @Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId);

        // ===================== Admin Dashboard Queries =====================

        // Count all orders created for an organization within a business day
        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId")
        long countTodayOrdersByOwnerOrg(@Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId);

        // Count orders by status for a business day
        @Query("SELECT COUNT(o) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.status = :status")
        long countTodayOrdersByOwnerOrgAndStatus(@Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        // Sum delivered amount for a business day (DELIVERED + PARTIAL_DELIVERY)
        @Query("SELECT COALESCE(SUM(CASE " +
                        "WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
                        "WHEN o.status = 'PARTIAL_DELIVERY' THEN COALESCE(o.partialDeliveryAmount, 0) " +
                        "ELSE 0 END), 0) " +
                        "FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId " +
                        "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
        java.math.BigDecimal sumTodayDeliveredAmountByOwnerOrg(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        // Get recent orders for an organization within a business day
        @Query("SELECT o FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId ORDER BY o.updatedAt DESC NULLS LAST, o.createdAt DESC")
        List<Order> findTodayOrdersByOwnerOrg(@Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId);

        // Count distinct couriers who have orders today for this organization
        @Query("SELECT COUNT(DISTINCT o.assignedToCourier.id) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.assignedToCourier IS NOT NULL")
        long countActiveCouriersToday(@Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId);

        // Count distinct organizations we sent orders to today
        @Query("SELECT COUNT(DISTINCT o.assignedToOrganization.id) FROM Order o WHERE o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId AND o.assignedToOrganization IS NOT NULL")
        long countActiveAssignedOrgsToday(@Param("orgId") Long orgId, @Param("businessDayId") Long businessDayId);

        // Count all orders assigned to a courier for a specific business day
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId AND o.ownerOrganization.id = :ownerOrgId")
        long countByAssignedToCourierIdAndBusinessDayId(
                @Param("courierId") Long courierId, 
                @Param("businessDayId") Long businessDayId,
                @Param("ownerOrgId") Long ownerOrgId);

        // Count orders assigned to a courier for a specific business day
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToCourier.id = :courierId AND o.businessDay.id = :businessDayId AND o.ownerOrganization.id = :ownerOrgId AND o.status = :status")
        long countByAssignedToCourierIdAndBusinessDayIdAndStatus(
                @Param("courierId") Long courierId, 
                @Param("businessDayId") Long businessDayId, 
                @Param("ownerOrgId") Long ownerOrgId,
                @Param("status") OrderStatus status);

        // Count all orders assigned to an organization for a specific business day
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :assignedOrgId AND o.businessDay.id = :businessDayId AND o.ownerOrganization.id = :ownerOrgId")
        long countByAssignedToOrgIdAndBusinessDayId(
                @Param("assignedOrgId") Long assignedOrgId, 
                @Param("businessDayId") Long businessDayId,
                @Param("ownerOrgId") Long ownerOrgId);

        // Count orders assigned to an organization for a specific business day with status
        @Query("SELECT COUNT(o) FROM Order o WHERE o.assignedToOrganization.id = :assignedOrgId AND o.businessDay.id = :businessDayId AND o.ownerOrganization.id = :ownerOrgId AND o.status = :status")
        long countByAssignedToOrgIdAndBusinessDayIdAndStatus(
                @Param("assignedOrgId") Long assignedOrgId, 
                @Param("businessDayId") Long businessDayId, 
                @Param("ownerOrgId") Long ownerOrgId,
                @Param("status") OrderStatus status);

        // ===================== Dashboard Combined Queries (Owner + Assigned) =====================

        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
                        "WHERE (o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)")
        long countAllOrdersForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
                        "WHERE ((o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "AND o.status = :status")
        long countAllOrdersForOrgDashboardByStatus(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId, @Param("status") OrderStatus status);

        @Query("SELECT COALESCE(SUM(CASE " +
                        "WHEN o.status = 'DELIVERED' THEN COALESCE(o.collectedAmount, o.amount) " +
                        "WHEN o.status = 'PARTIAL_DELIVERY' THEN COALESCE(o.partialDeliveryAmount, 0) " +
                        "ELSE 0 END), 0) " +
                        "FROM Order o " +
                        "WHERE ((o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "AND (o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY')")
        java.math.BigDecimal sumAllDeliveredAmountForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT DISTINCT o FROM Order o " +
                        "WHERE (o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true) " +
                        "ORDER BY o.updatedAt DESC NULLS LAST, o.createdAt DESC")
        List<Order> findAllOrdersForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.assignedToCourier.id) FROM Order o " +
                        "WHERE ((o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "AND o.assignedToCourier IS NOT NULL")
        long countAllActiveCouriersForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.assignedToOrganization.id) FROM Order o " +
                        "WHERE ((o.ownerOrganization.id = :orgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "AND o.assignedToOrganization IS NOT NULL AND o.assignedToOrganization.id != :orgId")
        long countAllActiveAssignedOrgsForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
                        "WHERE EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :orgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true) " +
                        "AND o.ownerOrganization.id != :orgId")
        long countAssignedOrdersForOrgDashboard(@Param("orgId") Long orgId,
                        @Param("businessDayId") Long businessDayId);

        // Courier performance in org dashboard context (owned + assigned)
        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
                        "WHERE o.assignedToCourier.id = :courierId " +
                        "AND ((o.ownerOrganization.id = :ownerOrgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :ownerOrgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true))")
        long countCourierOrdersForOrgDashboard(@Param("courierId") Long courierId,
                        @Param("ownerOrgId") Long ownerOrgId, @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o " +
                        "WHERE o.assignedToCourier.id = :courierId " +
                        "AND ((o.ownerOrganization.id = :ownerOrgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :ownerOrgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "AND o.status = :status")
        long countCourierOrdersForOrgDashboardByStatus(@Param("courierId") Long courierId,
                        @Param("ownerOrgId") Long ownerOrgId, @Param("businessDayId") Long businessDayId,
                        @Param("status") OrderStatus status);

        // Organization performance via OrderAssignment (assigner's business day)
        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN OrderAssignment oa ON oa.order = o " +
                        "WHERE oa.assignerOrganization.id = :assignerOrgId AND oa.assigneeOrganization.id = :assigneeOrgId " +
                        "AND oa.assignerBusinessDay.id = :businessDayId")
        long countOrgPerformanceByBusinessDay(@Param("assignerOrgId") Long assignerOrgId,
                        @Param("assigneeOrgId") Long assigneeOrgId, @Param("businessDayId") Long businessDayId);

        @Query("SELECT COUNT(DISTINCT o.id) FROM Order o JOIN OrderAssignment oa ON oa.order = o " +
                        "WHERE oa.assignerOrganization.id = :assignerOrgId AND oa.assigneeOrganization.id = :assigneeOrgId " +
                        "AND oa.assignerBusinessDay.id = :businessDayId AND o.status = :status")
        long countOrgPerformanceByBusinessDayAndStatus(@Param("assignerOrgId") Long assignerOrgId,
                        @Param("assigneeOrgId") Long assigneeOrgId, @Param("businessDayId") Long businessDayId,
                        @Param("status") OrderStatus status);

        // Aggregated dashboard queries
        @Query("SELECT o.assignedToCourier.id as courierId, " +
                        "o.assignedToCourier.fullName as courierFullName, " +
                        "o.assignedToCourier.username as courierUsername, " +
                        "COUNT(o.id) as totalOrders, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
                        "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedOrders " +
                        "FROM Order o WHERE o.assignedToCourier IS NOT NULL " +
                        "AND ((o.ownerOrganization.id = :ownerOrgId AND o.businessDay.id = :businessDayId) " +
                        "OR EXISTS (SELECT 1 FROM OrderAssignment oa WHERE oa.order = o " +
                        "AND oa.assigneeOrganization.id = :ownerOrgId AND oa.businessDay.id = :businessDayId AND oa.accepted = true)) " +
                        "GROUP BY o.assignedToCourier.id, o.assignedToCourier.fullName, o.assignedToCourier.username")
        List<com.shipment.shippinggo.dto.CourierPerformanceQueryResult> getCourierPerformancesForDashboard(
                @Param("ownerOrgId") Long ownerOrgId, @Param("businessDayId") Long businessDayId);

        @Query("SELECT oa.assigneeOrganization.id as organizationId, " +
                        "oa.assigneeOrganization.name as organizationName, " +
                        "oa.assigneeOrganization.type as organizationType, " +
                        "COUNT(DISTINCT o.id) as totalOrders, " +
                        "SUM(CASE WHEN o.status = 'DELIVERED' OR o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as deliveredOrders, " +
                        "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedOrders " +
                        "FROM Order o JOIN OrderAssignment oa ON oa.order = o " +
                        "WHERE oa.assignerOrganization.id = :assignerOrgId AND oa.assignerBusinessDay.id = :businessDayId " +
                        "GROUP BY oa.assigneeOrganization.id, oa.assigneeOrganization.name, oa.assigneeOrganization.type")
        List<com.shipment.shippinggo.dto.OrganizationPerformanceQueryResult> getOrgPerformancesForDashboard(
                @Param("assignerOrgId") Long assignerOrgId, @Param("businessDayId") Long businessDayId);

        // Advanced filter methods for chain-of-custody
        @Query("SELECT DISTINCT o FROM Order o " +
               "LEFT JOIN FETCH o.ownerOrganization " +
               "LEFT JOIN FETCH o.assignedToOrganization " +
               "LEFT JOIN FETCH o.assignedToCourier " +
               "LEFT JOIN FETCH o.businessDay " +
               "WHERE (o.businessDay.id = :businessDayId OR EXISTS (" +
               "   SELECT 1 FROM OrderAssignment oa WHERE oa.order = o AND oa.businessDay.id = :businessDayId AND oa.accepted = true" +
               ")) " +
               "AND (:search IS NULL OR (" +
               "   LOWER(o.recipientName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
               "   LOWER(o.recipientPhone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
               "   LOWER(o.recipientAddress) LIKE LOWER(CONCAT('%', :search, '%'))" +
               ")) " +
               "AND (:code IS NULL OR LOWER(o.code) LIKE LOWER(CONCAT('%', :code, '%'))) " +
               "AND (:courierId IS NULL OR o.assignedToCourier.id = :courierId) " +
                "AND (:governorate IS NULL OR o.governorate = :governorate) " +
                "AND (:status IS NULL OR o.status = :status) " +
                "AND (:incomingFromId IS NULL OR EXISTS (" +
               "   SELECT 1 FROM OrderAssignment oa2 WHERE oa2.order = o AND oa2.assigneeOrganization.id = :orgId AND oa2.assignerOrganization.id = :incomingFromId" +
               ")) " +
               "AND (:outgoingToId IS NULL OR EXISTS (" +
               "   SELECT 1 FROM OrderAssignment oa3 WHERE oa3.order = o AND oa3.assignerOrganization.id = :orgId AND oa3.assigneeOrganization.id = :outgoingToId" +
               "))")
        List<Order> findOrdersByBusinessDayWithFullFilters(
                @Param("businessDayId") Long businessDayId,
                @Param("orgId") Long orgId,
                @Param("search") String search,
                @Param("code") String code,
                @Param("courierId") Long courierId,
                @Param("status") OrderStatus status,
                @Param("governorate") Governorate governorate,
                @Param("incomingFromId") Long incomingFromId,
                @Param("outgoingToId") Long outgoingToId);
                
}
