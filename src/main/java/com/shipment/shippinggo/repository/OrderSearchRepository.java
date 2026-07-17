package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository متخصص في استعلامات البحث والفلترة المعقدة للطلبات.
 * تم نقل هذه الاستعلامات من OrderRepository لتقليل حجمه وتحسين التنظيم.
 */
@Repository
public interface OrderSearchRepository extends JpaRepository<Order, Long> {

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
            "(" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ") AND " +
            "(o.ownerOrganization.id = :orgId OR o.assignedToOrganization.id = :orgId OR o.creatorOrganization.id = :orgId)")
    List<Order> findOrdersWithFilters(
            @Param("orgId") Long orgId,
            @Param("code") String code,
            @Param("courierId") Long courierId,
            @Param("officeId") Long officeId,
            @Param("status") OrderStatus status,
            @Param("governorate") Governorate governorate,
            @Param("noGovernorate") Boolean noGovernorate);

    @Query("SELECT DISTINCT o FROM Order o " +
            "LEFT JOIN FETCH o.ownerOrganization " +
            "LEFT JOIN FETCH o.assignedToOrganization " +
            "LEFT JOIN FETCH o.assignedToCourier " +
            "LEFT JOIN FETCH o.businessDay " +
            "WHERE " +
            "(o.businessDay.id = :businessDayId OR EXISTS (" +
            "   SELECT 1 FROM OrderAssignment oa WHERE oa.order = o AND oa.businessDay.id = :businessDayId AND oa.accepted = true" +
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
            "(" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ")")
    List<Order> findOrdersByBusinessDayWithFilters(
            @Param("businessDayId") Long businessDayId,
            @Param("search") String search,
            @Param("code") String code,
            @Param("courierId") Long courierId,
            @Param("officeId") Long officeId,
            @Param("status") OrderStatus status,
            @Param("governorate") Governorate governorate,
            @Param("noGovernorate") Boolean noGovernorate);

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
            "(" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ") AND " +
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
            @Param("governorate") Governorate governorate,
            @Param("noGovernorate") Boolean noGovernorate,
            @Param("incomingFromId") Long incomingFromId,
            @Param("outgoingToId") Long outgoingToId);

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
            "AND (" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ") " +
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
            @Param("noGovernorate") Boolean noGovernorate,
            @Param("incomingFromId") Long incomingFromId,
            @Param("outgoingToId") Long outgoingToId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(o) as totalCount, " +
            "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as deliveredCount, " +
            "SUM(CASE WHEN o.status = 'PARTIAL_DELIVERY' THEN 1 ELSE 0 END) as partialCount, " +
            "SUM(CASE WHEN o.status = 'WAITING' THEN 1 ELSE 0 END) as waitingCount, " +
            "SUM(CASE WHEN o.status IN ('IN_TRANSIT', 'PICKED_UP', 'OUT_FOR_DELIVERY') THEN 1 ELSE 0 END) as inTransitCount, " +
            "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledCount, " +
            "SUM(CASE WHEN o.status = 'REFUSED' THEN 1 ELSE 0 END) as refusedCount, " +
            "SUM(CASE WHEN o.status = 'DEFERRED' THEN 1 ELSE 0 END) as deferredCount, " +
            "COALESCE(SUM(o.amount), 0) as totalAmount, " +
            "COALESCE(SUM(CASE WHEN o.status = 'DELIVERED' THEN o.amount WHEN o.status = 'PARTIAL_DELIVERY' THEN o.partialDeliveryAmount ELSE 0 END), 0) as deliveredAmount " +
            "FROM Order o " +
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
            "AND (" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ") " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:incomingFromId IS NULL OR EXISTS (" +
            "   SELECT 1 FROM OrderAssignment oa2 WHERE oa2.order = o AND oa2.assigneeOrganization.id = :orgId AND oa2.assignerOrganization.id = :incomingFromId" +
            ")) " +
            "AND (:outgoingToId IS NULL OR EXISTS (" +
            "   SELECT 1 FROM OrderAssignment oa3 WHERE oa3.order = o AND oa3.assignerOrganization.id = :orgId AND oa3.assigneeOrganization.id = :outgoingToId" +
            "))")
    com.shipment.shippinggo.dto.BusinessDayStatsQueryResult getBusinessDayStatsWithFullFilters(
            @Param("businessDayId") Long businessDayId,
            @Param("orgId") Long orgId,
            @Param("search") String search,
            @Param("code") String code,
            @Param("courierId") Long courierId,
            @Param("status") OrderStatus status,
            @Param("governorate") Governorate governorate,
            @Param("noGovernorate") Boolean noGovernorate,
            @Param("incomingFromId") Long incomingFromId,
            @Param("outgoingToId") Long outgoingToId);

    @Query("SELECT o.id FROM Order o " +
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
            "AND (" +
            "   (:noGovernorate = true AND o.governorate IS NULL) OR " +
            "   (:noGovernorate = false AND (:governorate IS NULL OR o.governorate = :governorate))" +
            ") " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:incomingFromId IS NULL OR EXISTS (" +
            "   SELECT 1 FROM OrderAssignment oa2 WHERE oa2.order = o AND oa2.assigneeOrganization.id = :orgId AND oa2.assignerOrganization.id = :incomingFromId" +
            ")) " +
            "AND (:outgoingToId IS NULL OR EXISTS (" +
            "   SELECT 1 FROM OrderAssignment oa3 WHERE oa3.order = o AND oa3.assignerOrganization.id = :orgId AND oa3.assigneeOrganization.id = :outgoingToId" +
            ")) " +
            "AND o.status <> 'OUT_FOR_DELIVERY'")
    List<Long> findBulkAssignableOrderIdsByBusinessDayWithFullFilters(
            @Param("businessDayId") Long businessDayId,
            @Param("orgId") Long orgId,
            @Param("search") String search,
            @Param("code") String code,
            @Param("courierId") Long courierId,
            @Param("status") OrderStatus status,
            @Param("governorate") Governorate governorate,
            @Param("noGovernorate") Boolean noGovernorate,
            @Param("incomingFromId") Long incomingFromId,
            @Param("outgoingToId") Long outgoingToId);
}
