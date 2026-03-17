package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Invoice;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.InvoiceRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    public List<Order> getOrdersByInvoiceId(Long invoiceId) {
        return orderRepository.findByInvoiceId(invoiceId);
    }

    public List<Invoice> getInvoicesByOrganization(Long orgId) {
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }
    
    @Transactional
    public Invoice generateInvoice(Organization organization) {
        List<Order> unInvoicedOrders = orderRepository.findByOwnerOrganizationIdAndStatusAndInvoiceIsNull(organization.getId(), OrderStatus.DELIVERED);
        
        if (unInvoicedOrders.isEmpty()) {
            throw new RuntimeException("No delivered un-invoiced orders found for this organization.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Order order : unInvoicedOrders) {
            totalAmount = totalAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .organization(organization)
                .totalAmount(totalAmount)
                .status("UNPAID")
                .build();

        final Invoice savedInvoice = invoiceRepository.save(invoice);

        for (Order order : unInvoicedOrders) {
            order.setInvoice(savedInvoice);
            orderRepository.save(order);
        }

        return savedInvoice;
    }

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }
}
