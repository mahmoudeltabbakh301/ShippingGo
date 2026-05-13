package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.InvoiceReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceReceiptRepository extends JpaRepository<InvoiceReceipt, Long> {

    List<InvoiceReceipt> findByInvoiceIdOrderByConfirmationOrderAsc(Long invoiceId);

    Optional<InvoiceReceipt> findByInvoiceIdAndOrganizationId(Long invoiceId, Long organizationId);

    // هل كل المنظمات أكدت الاستلام لفاتورة معينة
    @Query("SELECT CASE WHEN COUNT(ir) = 0 THEN true ELSE false END FROM InvoiceReceipt ir " +
           "WHERE ir.invoice.id = :invoiceId AND ir.confirmed = false")
    boolean isFullyConfirmed(@Param("invoiceId") Long invoiceId);

    // عدد التأكيدات المكتملة
    @Query("SELECT COUNT(ir) FROM InvoiceReceipt ir WHERE ir.invoice.id = :invoiceId AND ir.confirmed = true")
    long countConfirmedByInvoiceId(@Param("invoiceId") Long invoiceId);

    // عدد التأكيدات الإجمالية
    long countByInvoiceId(Long invoiceId);

    // حذف تأكيدات فاتورة
    void deleteByInvoiceId(Long invoiceId);
}
