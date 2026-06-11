package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByOrganizationId(Long organizationId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    // الاشتراكات التجريبية المنتهية التي لم يتم تعليقها بعد
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate < :now")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);

    // الاشتراكات النشطة المنتهية
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd < :now")
    List<Subscription> findExpiredActive(@Param("now") LocalDateTime now);

    // الاشتراكات التي ستنتهي خلال فترة (للتنبيهات)
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate BETWEEN :from AND :to")
    List<Subscription> findExpiringTrials(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.currentPeriodEnd BETWEEN :from AND :to")
    List<Subscription> findExpiringActive(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // إحصائيات للسوبر أدمن
    long countByStatus(SubscriptionStatus status);

    @Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate > :now")
    long countActiveTrials(@Param("now") LocalDateTime now);

    boolean existsByOrganizationId(Long organizationId);
}
