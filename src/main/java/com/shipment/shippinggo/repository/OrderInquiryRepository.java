package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.OrderInquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderInquiryRepository extends JpaRepository<OrderInquiry, Long> {

    // استعلامات واردة لمنظمة مرتبطة بيوم عمل مع التحميل المسبق
    @Query("SELECT oi FROM OrderInquiry oi " +
            "LEFT JOIN FETCH oi.order o " +
            "LEFT JOIN FETCH o.ownerOrganization " +
            "LEFT JOIN FETCH o.assignedToOrganization " +
            "LEFT JOIN FETCH o.assignedToCourier " +
            "LEFT JOIN FETCH oi.senderOrganization " +
            "WHERE oi.receiverOrganization.id = :orgId AND oi.businessDay.id = :bdId " +
            "ORDER BY oi.sentAt DESC")
    List<OrderInquiry> findByReceiverOrganizationIdAndBusinessDayId(@Param("orgId") Long orgId,
            @Param("bdId") Long bdId);

    // التحقق من وجود استعلام مسبق لنفس الأوردر والمنظمة
    boolean existsByOrderIdAndReceiverOrganizationId(Long orderId, Long receiverOrganizationId);

    // جلب استعلام محدد بأوردر ومنظمة مستلمة
    Optional<OrderInquiry> findByOrderIdAndReceiverOrganizationId(Long orderId, Long receiverOrganizationId);

    // جلب كل استعلامات أوردر معين
    List<OrderInquiry> findByOrderId(Long orderId);

    // الاستعلامات المرسلة من منظمة
    List<OrderInquiry> findBySenderOrganizationId(Long senderOrganizationId);

    // حذف استعلامات أوردر عند حذفه
    @Modifying
    void deleteByOrderId(Long orderId);

    // عدد الاستعلامات الواردة ليوم عمل
    long countByReceiverOrganizationIdAndBusinessDayId(Long orgId, Long bdId);
}
