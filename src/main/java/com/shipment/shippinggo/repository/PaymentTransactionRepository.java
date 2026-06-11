package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.PaymentTransaction;
import com.shipment.shippinggo.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);

    Optional<PaymentTransaction> findByPaymobOrderId(String paymobOrderId);

    Optional<PaymentTransaction> findByPaymobTransactionId(String paymobTransactionId);

    Page<PaymentTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PaymentTransaction> findByStatusOrderByCreatedAtDesc(PaymentStatus status, Pageable pageable);

    // إجمالي الإيرادات
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status = 'SUCCESS'")
    BigDecimal getTotalRevenue();

    // إجمالي الإيرادات هذا الشهر
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status = 'SUCCESS' AND MONTH(p.paidAt) = MONTH(CURRENT_DATE) AND YEAR(p.paidAt) = YEAR(CURRENT_DATE)")
    BigDecimal getMonthlyRevenue();

    long countByStatus(PaymentStatus status);
}
