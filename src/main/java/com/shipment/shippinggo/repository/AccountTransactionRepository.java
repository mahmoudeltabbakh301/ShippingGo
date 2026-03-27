package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.AccountTransaction;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    // معاملات منظمة معينة
    List<AccountTransaction> findByOrganizationOrderByCreatedAtDesc(Organization organization);

    // معاملات مندوب معين
    List<AccountTransaction> findByCourierOrderByCreatedAtDesc(User courier);

    // مجموع العمولات لمنظمة معينة
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AccountTransaction t WHERE t.organization = :organization")
    BigDecimal sumByOrganization(@Param("organization") Organization organization);

    // مجموع العمولات لمندوب معين
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM AccountTransaction t WHERE t.courier = :courier")
    BigDecimal sumByCourier(@Param("courier") User courier);

    java.util.Optional<AccountTransaction> findTopByOrderIdAndCourierId(Long orderId, Long courierId);
    
    // جميع المعاملات المرتبطة بأوردر واحد
    List<AccountTransaction> findByOrderId(Long orderId);

    // عدد المعاملات لمنظمة معينة
    long countByOrganization(Organization organization);

    // عدد المعاملات لمندوب معين
    long countByCourier(User courier);
}
