package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.BusinessDayInvoiceSummaryDTO;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.InvoiceReceiptRepository;
import com.shipment.shippinggo.repository.InvoiceRepository;
import com.shipment.shippinggo.repository.OrderAssignmentRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final InvoiceReceiptRepository invoiceReceiptRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
            OrderRepository orderRepository,
            InvoiceReceiptRepository invoiceReceiptRepository,
            OrderAssignmentRepository orderAssignmentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
        this.invoiceReceiptRepository = invoiceReceiptRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
    }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    public List<Invoice> getInvoicesByOrganization(Long orgId) {
        return invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    public List<Invoice> getInvoicesByBusinessDay(Long orgId, Long businessDayId) {
        return invoiceRepository.findByOrganizationIdAndBusinessDayId(orgId, businessDayId);
    }

    public List<Invoice> searchInvoicesByCode(Long orgId, Long businessDayId, String code) {
        return invoiceRepository.searchByCodeInBusinessDay(orgId, businessDayId, code);
    }

    public List<Invoice> getAllInvoicesGroupedByBusinessDay(Long orgId) {
        return invoiceRepository.findAllByOrganizationWithBusinessDay(orgId);
    }

    /**
     * ملخصات أيام العمل مع عدد الفواتير والإجمالي (بدون تحميل الفواتير)
     */
    public List<BusinessDayInvoiceSummaryDTO> getBusinessDaySummaries(Long orgId) {
        return invoiceRepository.findBusinessDayInvoiceSummaries(orgId);
    }

    /**
     * جلب فواتير يوم عمل محدد مع تقسيم الصفحات
     */
    public Page<Invoice> getInvoicesByBusinessDayPaged(Long orgId, Long businessDayId, int page, int size) {
        return invoiceRepository.findByOrganizationIdAndBusinessDayIdPageable(orgId, businessDayId,
                PageRequest.of(page, size));
    }

    public Page<Invoice> searchInvoicesPaged(Long orgId, Long businessDayId, String code, Long courierId, int page, int size) {
        return invoiceRepository.searchInvoicesPaged(orgId, businessDayId, code, courierId, PageRequest.of(page, size));
    }

    /**
     * إنشاء فاتورة لأوردر واحد داخل يوم عمل
     */
    @Transactional
    public Invoice generateInvoiceForOrder(Organization org, BusinessDay businessDay, Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        // التحقق من عدم وجود فاتورة مسبقة لهذا الأوردر في نفس يوم العمل
        if (invoiceRepository.existsByOrderIdAndBusinessDayId(orderId, businessDay.getId())) {
            throw new BusinessLogicException("يوجد بالفعل فاتورة لهذا الطلب في يوم العمل هذا");
        }

        BigDecimal totalAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;

        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .organization(org)
                .businessDay(businessDay)
                .order(order)
                .totalAmount(totalAmount)
                .createdBy(user)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // نظام تأكيد الاستلام معطّل - تم إلغاؤه لتمكين المنظمات من التحكم بدون قيود
        // createReceiptChain(savedInvoice, order);

        return savedInvoice;
    }

    /**
     * إنشاء فواتير لمجموعة أوردرات (كل أوردر فاتورة منفصلة)
     */
    @Transactional
    public List<Invoice> generateInvoicesForOrders(Organization org, BusinessDay businessDay, List<Long> orderIds,
            User user) {
        List<Invoice> invoices = new ArrayList<>();
        for (Long orderId : orderIds) {
            try {
                Invoice invoice = generateInvoiceForOrder(org, businessDay, orderId, user);
                invoices.add(invoice);
            } catch (BusinessLogicException e) {
                // تخطي الأوردرات التي لديها فاتورة بالفعل
            }
        }
        if (invoices.isEmpty()) {
            throw new BusinessLogicException(
                    "لم يتم إنشاء أي فاتورة. قد تكون جميع الطلبات المحددة لديها فواتير بالفعل.");
        }
        return invoices;
    }

    /**
     * إنشاء سلسلة تأكيد الاستلام بناءً على سلسلة الإسناد
     * الترتيب: من آخر طرف مسند إليه إلى المالك الأصلي
     */
    private void createReceiptChain(Invoice invoice, Order order) {
        List<OrderAssignment> chain = orderAssignmentRepository.findByOrderIdOrderByLevelAsc(order.getId());

        int confirmationOrder = 1;

        // من آخر طرف مسند إليه (أعلى level) إلى الأول
        // ترتيب عكسي: آخر assignee يؤكد أولاً
        for (int i = chain.size() - 1; i >= 0; i--) {
            OrderAssignment assignment = chain.get(i);
            Organization assigneeOrg = assignment.getAssigneeOrganization();

            // تجنب التكرار
            InvoiceReceipt receipt = InvoiceReceipt.builder()
                    .invoice(invoice)
                    .organization(assigneeOrg)
                    .confirmationOrder(confirmationOrder++)
                    .confirmed(false)
                    .build();
            invoiceReceiptRepository.save(receipt);
        }

        // المالك الأصلي يؤكد آخراً
        InvoiceReceipt ownerReceipt = InvoiceReceipt.builder()
                .invoice(invoice)
                .organization(order.getOwnerOrganization())
                .confirmationOrder(confirmationOrder)
                .confirmed(false)
                .build();
        invoiceReceiptRepository.save(ownerReceipt);
    }

    /**
     * تأكيد استلام الفاتورة من منظمة
     */
    @Transactional
    public void confirmReceipt(Long invoiceId, Organization org, User user) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("الفاتورة غير موجودة"));

        // جلب سلسلة التأكيد
        List<InvoiceReceipt> receipts = invoiceReceiptRepository
                .findByInvoiceIdOrderByConfirmationOrderAsc(invoiceId);

        // التحقق من أن هذه المنظمة في السلسلة
        InvoiceReceipt myReceipt = receipts.stream()
                .filter(r -> r.getOrganization().getId().equals(org.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessLogicException("منظمتك ليست في سلسلة تأكيد هذه الفاتورة"));

        // التحقق من عدم التأكيد المسبق
        if (myReceipt.isConfirmed()) {
            throw new BusinessLogicException("تم تأكيد الاستلام مسبقاً من منظمتك");
        }

        // التحقق من أن كل الأطراف السابقة أكدت (الترتيب)
        for (InvoiceReceipt receipt : receipts) {
            if (receipt.getConfirmationOrder() < myReceipt.getConfirmationOrder() && !receipt.isConfirmed()) {
                throw new BusinessLogicException("يجب أن يتم تأكيد الاستلام من الطرف السابق أولاً ("
                        + receipt.getOrganization().getName() + ")");
            }
        }

        // تأكيد الاستلام
        myReceipt.setConfirmed(true);
        myReceipt.setConfirmedAt(LocalDateTime.now());
        myReceipt.setConfirmedBy(user);
        invoiceReceiptRepository.save(myReceipt);
    }

    /**
     * جلب تأكيدات الاستلام لفاتورة
     */
    public List<InvoiceReceipt> getReceiptsForInvoice(Long invoiceId) {
        return invoiceReceiptRepository.findByInvoiceIdOrderByConfirmationOrderAsc(invoiceId);
    }

    /**
     * هل الفاتورة مكتملة التأكيد
     */
    public boolean isInvoiceFullyConfirmed(Long invoiceId) {
        return invoiceReceiptRepository.isFullyConfirmed(invoiceId);
    }

    /**
     * هل يحق لهذه المنظمة تأكيد استلام الفاتورة الآن
     */
    public boolean canConfirmReceipt(Long invoiceId, Long orgId) {
        List<InvoiceReceipt> receipts = invoiceReceiptRepository
                .findByInvoiceIdOrderByConfirmationOrderAsc(invoiceId);

        InvoiceReceipt myReceipt = receipts.stream()
                .filter(r -> r.getOrganization().getId().equals(orgId))
                .findFirst()
                .orElse(null);

        if (myReceipt == null || myReceipt.isConfirmed()) {
            return false;
        }

        // التحقق من أن كل الأطراف السابقة أكدت
        for (InvoiceReceipt receipt : receipts) {
            if (receipt.getConfirmationOrder() < myReceipt.getConfirmationOrder() && !receipt.isConfirmed()) {
                return false;
            }
        }

        return true;
    }

    @Transactional
    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    /**
     * التحقق من وجود فاتورة لأوردر
     */
    public boolean hasInvoice(Long orderId) {
        return invoiceRepository.existsByOrderId(orderId);
    }

    // ===== Deprecated methods kept for backward compatibility =====

    public List<Order> getOrdersByInvoiceId(Long invoiceId) {
        return orderRepository.findByInvoiceId(invoiceId);
    }

    @Transactional
    public Invoice generateInvoice(Organization organization) {
        throw new UnsupportedOperationException("استخدم generateInvoiceForOrder بدلاً من هذه الميثود");
    }
}
