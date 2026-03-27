package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
