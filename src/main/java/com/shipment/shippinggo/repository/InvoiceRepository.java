package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.dto.BusinessDayInvoiceSummaryDTO;
import com.shipment.shippinggo.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

       List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

       // ملخصات أيام العمل التي تحتوي فواتير (بدون تحميل الفواتير نفسها)
       @Query("SELECT new com.shipment.shippinggo.dto.BusinessDayInvoiceSummaryDTO(" +
                     "bd.id, bd.name, bd.date, bd.active, COUNT(i), COALESCE(SUM(i.totalAmount), 0)) " +
                     "FROM Invoice i JOIN i.businessDay bd " +
                     "WHERE i.organization.id = :orgId " +
                     "GROUP BY bd.id, bd.name, bd.date, bd.active " +
                     "ORDER BY bd.date DESC")
       List<BusinessDayInvoiceSummaryDTO> findBusinessDayInvoiceSummaries(@Param("orgId") Long orgId);

       // فواتير يوم عمل معين
       @Query("SELECT i FROM Invoice i " +
                     "LEFT JOIN FETCH i.order " +
                     "LEFT JOIN FETCH i.businessDay " +
                     "WHERE i.organization.id = :orgId AND i.businessDay.id = :businessDayId " +
                     "ORDER BY i.createdAt DESC")
       List<Invoice> findByOrganizationIdAndBusinessDayId(
                     @Param("orgId") Long orgId,
                     @Param("businessDayId") Long businessDayId);

       // فواتير يوم عمل معين - مع تقسيم الصفحات
       @Query(value = "SELECT i FROM Invoice i " +
                     "LEFT JOIN FETCH i.order " +
                     "LEFT JOIN FETCH i.businessDay " +
                     "WHERE i.organization.id = :orgId AND i.businessDay.id = :businessDayId " +
                     "ORDER BY i.createdAt DESC",
              countQuery = "SELECT COUNT(i) FROM Invoice i " +
                     "WHERE i.organization.id = :orgId AND i.businessDay.id = :businessDayId")
       Page<Invoice> findByOrganizationIdAndBusinessDayIdPageable(
                     @Param("orgId") Long orgId,
                     @Param("businessDayId") Long businessDayId,
                     Pageable pageable);

       // بحث بكود الأوردر
       @Query("SELECT i FROM Invoice i " +
                     "LEFT JOIN FETCH i.order o " +
                     "LEFT JOIN FETCH i.businessDay " +
                     "WHERE i.organization.id = :orgId AND i.businessDay.id = :businessDayId " +
                     "AND (o.code LIKE CONCAT('%', :code, '%') OR i.invoiceNumber LIKE CONCAT('%', :code, '%')) " +
                     "ORDER BY i.createdAt DESC")
       List<Invoice> searchByCodeInBusinessDay(
                     @Param("orgId") Long orgId,
                     @Param("businessDayId") Long businessDayId,
                     @Param("code") String code);

       // هل يوجد فاتورة لهذا الأوردر
       boolean existsByOrderId(Long orderId);

       // هل يوجد فاتورة لهذا الأوردر في يوم عمل محدد
       boolean existsByOrderIdAndBusinessDayId(Long orderId, Long businessDayId);

       // جلب فاتورة بالأوردر
       Optional<Invoice> findByOrderId(Long orderId);

       // كل فواتير منظمة مع يوم العمل (مجمعة)
    @Query("SELECT i FROM Invoice i " +
           "LEFT JOIN FETCH i.order " +
           "LEFT JOIN FETCH i.businessDay bd " +
           "WHERE i.organization.id = :orgId " +
           "ORDER BY bd.date DESC, i.createdAt DESC")
    List<Invoice> findAllByOrganizationWithBusinessDay(@Param("orgId") Long orgId);

    @Query("SELECT i.id FROM Invoice i " +
           "LEFT JOIN i.order o " +
           "WHERE i.organization.id = :orgId AND i.businessDay.id = :businessDayId " +
           "AND (:code IS NULL OR :code = '' OR o.code LIKE CONCAT('%', :code, '%') OR i.invoiceNumber LIKE CONCAT('%', :code, '%')) " +
           "AND (:courierId IS NULL OR o.assignedToCourier.id = :courierId) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (:governorate IS NULL OR o.governorate = :governorate)")
    List<Long> findInvoiceIdsWithFilters(
            @Param("orgId") Long orgId,
            @Param("businessDayId") Long businessDayId,
            @Param("code") String code,
            @Param("courierId") Long courierId,
            @Param("status") com.shipment.shippinggo.enums.OrderStatus status,
            @Param("governorate") com.shipment.shippinggo.enums.Governorate governorate);
}