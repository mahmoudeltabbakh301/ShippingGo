package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.AppNotification;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByUserOrderByCreatedAtDesc(User user);
    
    Page<AppNotification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // آخر N إشعارات للعرض في الـ dropdown
    List<AppNotification> findTop10ByUserOrderByCreatedAtDesc(User user);

    long countByUserAndIsReadFalse(User user);

    @Modifying
    @Query("UPDATE AppNotification n SET n.isRead = true WHERE n.user = :user AND n.isRead = false")
    int markAllAsReadByUser(@Param("user") User user);

    // تنظيف تلقائي - حذف الإشعارات الأقدم من تاريخ محدد
    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    // حذف الإشعارات المقروءة فقط الأقدم من تاريخ محدد
    @Modifying
    @Query("DELETE FROM AppNotification n WHERE n.isRead = true AND n.createdAt < :cutoffDate")
    int deleteReadOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
