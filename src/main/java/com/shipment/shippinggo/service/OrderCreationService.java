package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import java.math.BigDecimal;

@Service
public class OrderCreationService {

    private static final Logger log = LoggerFactory.getLogger(OrderCreationService.class);

    private final OrderRepository orderRepository;
    private final BusinessDayRepository businessDayRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;
    private final OrderEventRepository orderEventRepository;
    private final OrderInquiryRepository orderInquiryRepository;
    private final QrCodeService qrCodeService;
    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final MembershipRepository membershipRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;
    private final com.shipment.shippinggo.repository.ClientOrgRepository clientOrgRepository;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderStatusService orderStatusService;
    private final OrganizationResolverService organizationResolverService;
    private final InvoiceService invoiceService;

    public OrderCreationService(OrderRepository orderRepository,
            BusinessDayRepository businessDayRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            OrderEventRepository orderEventRepository,
            OrderInquiryRepository orderInquiryRepository,
            QrCodeService qrCodeService,
            CompanyRepository companyRepository,
            OfficeRepository officeRepository,
            StoreRepository storeRepository,
            MembershipRepository membershipRepository,
            VirtualOfficeRepository virtualOfficeRepository,
            com.shipment.shippinggo.repository.ClientOrgRepository clientOrgRepository,
            OrderAssignmentService orderAssignmentService,
            OrderStatusService orderStatusService,
            OrganizationResolverService organizationResolverService,
            InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.businessDayRepository = businessDayRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderInquiryRepository = orderInquiryRepository;
        this.qrCodeService = qrCodeService;
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.membershipRepository = membershipRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
        this.clientOrgRepository = clientOrgRepository;
        this.orderAssignmentService = orderAssignmentService;
        this.orderStatusService = orderStatusService;
        this.organizationResolverService = organizationResolverService;
        this.invoiceService = invoiceService;
    }

    // حفظ الطلب في قاعدة البيانات بدون أي منطق إضافي
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    // إنشاء طلب جديد برقم تتبع فريد وربطه بيوم عمل محدد
    @Transactional
    public Order createOrder(OrderDto dto, User createdBy, Organization ownerOrganization) {
        BusinessDay businessDay = businessDayRepository.findById(dto.getBusinessDayId())
                .orElseThrow(() -> new ResourceNotFoundException("Business day not found"));

        String orderCode = dto.getCode();
        if (orderCode == null || orderCode.trim().isEmpty()) {
            orderCode = qrCodeService.generateUniqueCode(ownerOrganization.getId());
        }

        Order order = Order.builder()
                .businessDay(businessDay)
                .code(orderCode)
                .sequenceNumber(dto.getSequenceNumber())
                .companyName(dto.getCompanyName())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .recipientAddress(dto.getRecipientAddress())
                .quantity(dto.getQuantity())
                .amount(dto.getAmount())
                .shippingPrice(dto.getShippingPrice())
                .orderPrice(dto.getOrderPrice())
                .notes(dto.getNotes())
                .governorate(dto.getGovernorate())
                .center(dto.getCenter())
                .area(dto.getArea())
                .district(getDistrictFallback(dto.getCenter(), dto.getArea()))
                .status(OrderStatus.WAITING)
                .createdBy(createdBy)
                .ownerOrganization(ownerOrganization)
                .creatorOrganization(ownerOrganization)
                .build();

        Order savedOrder = orderRepository.save(order);

        // إنشاء فاتورة تلقائياً للأوردر
        try {
            invoiceService.generateInvoiceForOrder(ownerOrganization, businessDay, savedOrder.getId(), createdBy);
        } catch (Exception e) {
            log.warn("فشل إنشاء فاتورة تلقائية للأوردر {}: {}", savedOrder.getId(), e.getMessage());
        }

        return savedOrder;
    }

    // إدخال طلبات متعددة دفعة واحدة (Batch Insert) لتحسين الأداء
    @Transactional
    public java.util.List<Order> createOrdersBulk(java.util.List<OrderDto> dtos, User createdBy, Organization ownerOrganization, BusinessDay businessDay) {
        java.util.List<Order> orders = new java.util.ArrayList<>();
        
        for (OrderDto dto : dtos) {
            String orderCode = dto.getCode();
            if (orderCode == null || orderCode.trim().isEmpty()) {
                orderCode = qrCodeService.generateUniqueCode(ownerOrganization.getId());
            }

            Order order = Order.builder()
                    .businessDay(businessDay)
                    .code(orderCode)
                    .sequenceNumber(dto.getSequenceNumber())
                    .companyName(dto.getCompanyName())
                    .recipientName(dto.getRecipientName())
                    .recipientPhone(dto.getRecipientPhone())
                    .recipientAddress(dto.getRecipientAddress())
                    .quantity(dto.getQuantity())
                    .amount(dto.getAmount())
                    .shippingPrice(dto.getShippingPrice())
                    .orderPrice(dto.getOrderPrice())
                    .notes(dto.getNotes())
                    .governorate(dto.getGovernorate())
                    .center(dto.getCenter())
                    .area(dto.getArea())
                    .district(getDistrictFallback(dto.getCenter(), dto.getArea()))
                    .status(OrderStatus.WAITING)
                    .createdBy(createdBy)
                    .ownerOrganization(ownerOrganization)
                    .creatorOrganization(ownerOrganization)
                    .build();

            orders.add(order);
        }

        java.util.List<Order> savedOrders = orderRepository.saveAll(orders);

        // إنشاء فواتير تلقائياً لجميع الأوردرات
        for (Order savedOrder : savedOrders) {
            try {
                invoiceService.generateInvoiceForOrder(ownerOrganization, businessDay, savedOrder.getId(), createdBy);
            } catch (Exception e) {
                log.warn("فشل إنشاء فاتورة تلقائية للأوردر {}: {}", savedOrder.getId(), e.getMessage());
            }
        }

        return savedOrders;
    }

    // تعديل بيانات الطلب مع الاحتفاظ بسجل التغييرات (Logs) والتحقق من الصلاحيات
    @Transactional
    public Order updateOrderDetails(Long orderId, OrderDto dto, User updatedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("الطلب غير موجود"));

        if (!orderAssignmentService.canUserAccessOrder(updatedBy, order)) {
            throw new UnauthorizedAccessException("غير مصرح بالوصول لهذا الطلب");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.REFUSED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessLogicException("لا يمكن تعديل بيانات طلب في حالة نهائية ("
                    + order.getStatus().getArabicName() + "). يجب إعادة الحالة إلى 'في الطريق' أولاً.");
        }

        boolean isOwner = orderAssignmentService.isUserMemberOfOrganization(updatedBy, order.getOwnerOrganization());
        boolean isAssignee = order.getAssignedToOrganization() != null &&
                orderAssignmentService.isUserMemberOfOrganization(updatedBy, order.getAssignedToOrganization());

        if (order.getAssignedToOrganization() != null) {
            if (isOwner && !isAssignee) {
                throw new BusinessLogicException(
                        "لا يمكن تعديل الطلب بعد إسناده للمكتب. التعديل متاح فقط للمكتب المستلم.");
            }
            if (!isAssignee) {
                throw new UnauthorizedAccessException("غير مصرح لك بتعديل هذا الطلب.");
            }
        } else {
            if (!isOwner) {
                throw new UnauthorizedAccessException("غير مصرح لك بتعديل هذا الطلب.");
            }
        }

        StringBuilder changesLog = new StringBuilder();
        BigDecimal oldAmount = order.getAmount();

        if (dto.getRecipientName() != null && !dto.getRecipientName().equals(order.getRecipientName())) {
            changesLog.append(String.format("تعديل اسم المستلم من '%s' إلى '%s'. ",
                    order.getRecipientName() != null ? order.getRecipientName() : "", dto.getRecipientName()));
            order.setRecipientName(dto.getRecipientName());
        }
        if (dto.getRecipientPhone() != null && !dto.getRecipientPhone().equals(order.getRecipientPhone())) {
            changesLog.append(String.format("تعديل رقم الهاتف من '%s' إلى '%s'. ",
                    order.getRecipientPhone() != null ? order.getRecipientPhone() : "", dto.getRecipientPhone()));
            order.setRecipientPhone(dto.getRecipientPhone());
        }
        if (dto.getRecipientAddress() != null && !dto.getRecipientAddress().equals(order.getRecipientAddress())) {
            changesLog.append("تعديل العنوان. ");
            order.setRecipientAddress(dto.getRecipientAddress());
        }

        if (dto.getShippingPrice() != null) {
            if (order.getShippingPrice() == null || !dto.getShippingPrice().equals(order.getShippingPrice())) {
                changesLog.append(String.format("تعديل سعر الشحن من %.2f إلى %.2f. ",
                        order.getShippingPrice() != null ? order.getShippingPrice() : BigDecimal.ZERO,
                        dto.getShippingPrice()));
                order.setShippingPrice(dto.getShippingPrice());
            }
        }

        if (dto.getOrderPrice() != null) {
            if (order.getOrderPrice() == null || !dto.getOrderPrice().equals(order.getOrderPrice())) {
                changesLog.append(String.format("تعديل سعر البضاعة من %.2f إلى %.2f. ",
                        order.getOrderPrice() != null ? order.getOrderPrice() : BigDecimal.ZERO, dto.getOrderPrice()));
                order.setOrderPrice(dto.getOrderPrice());
            }
        }

        if (dto.getAmount() != null) {
            if (order.getAmount() == null || !dto.getAmount().equals(order.getAmount())) {
                changesLog.append(String.format("تعديل السعر الإجمالي من %.2f إلى %.2f. ",
                        order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO, dto.getAmount()));
                order.setAmount(dto.getAmount());
            }
        } else {
            if (dto.getShippingPrice() != null || dto.getOrderPrice() != null) {
                BigDecimal sp = order.getShippingPrice() != null ? order.getShippingPrice() : BigDecimal.ZERO;
                BigDecimal op = order.getOrderPrice() != null ? order.getOrderPrice() : BigDecimal.ZERO;
                BigDecimal newAmount = sp.add(op);
                if (order.getAmount() == null || !newAmount.equals(order.getAmount())) {
                    changesLog.append(String.format("تعديل السعر الإجمالي بعد التقسيم من %.2f إلى %.2f. ",
                            order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO, newAmount));
                    order.setAmount(newAmount);
                }
            }
        }

        if (dto.getRejectionPayment() != null) {
            if (order.getRejectionPayment() == null || !dto.getRejectionPayment().equals(order.getRejectionPayment())) {
                changesLog.append(String.format("تعديل مبلغ الرفض من %.2f إلى %.2f. ",
                        order.getRejectionPayment() != null ? order.getRejectionPayment() : BigDecimal.ZERO,
                        dto.getRejectionPayment()));
                order.setRejectionPayment(dto.getRejectionPayment());
            }
        }

        if (dto.getNotes() != null && !dto.getNotes().equals(order.getNotes())) {
            order.setNotes(dto.getNotes());
        }

        if (dto.getGovernorate() != null && dto.getGovernorate() != order.getGovernorate()) {
            changesLog.append(String.format("تعديل المحافظة من '%s' إلى '%s'. ",
                    order.getGovernorate() != null ? order.getGovernorate().getArabicName() : "غير محدد",
                    dto.getGovernorate().getArabicName()));
            order.setGovernorate(dto.getGovernorate());
        }

        if (dto.getCenter() != null && !dto.getCenter().equals(order.getCenter())) {
            changesLog.append(String.format("تعديل المركز إلى '%s'. ", dto.getCenter()));
            order.setCenter(dto.getCenter());
            order.setDistrict(getDistrictFallback(dto.getCenter(), dto.getArea() != null ? dto.getArea() : order.getArea()));
        }

        if (dto.getArea() != null && !dto.getArea().equals(order.getArea())) {
            changesLog.append(String.format("تعديل المنطقة إلى '%s'. ", dto.getArea()));
            order.setArea(dto.getArea());
            order.setDistrict(getDistrictFallback(order.getCenter(), dto.getArea()));
        }

        if (dto.getQuantity() != null && !dto.getQuantity().equals(order.getQuantity())) {
            changesLog.append(String.format("تعديل الكمية من %d إلى %d. ",
                    order.getQuantity() != null ? order.getQuantity() : 0, dto.getQuantity()));
            order.setQuantity(dto.getQuantity());
        }

        if (dto.getCode() != null && !dto.getCode().equals(order.getCode())) {
            order.setCode(dto.getCode());
        }

        if (dto.getCompanyName() != null && !dto.getCompanyName().equals(order.getCompanyName())) {
            order.setCompanyName(dto.getCompanyName());
        }

        // تحديث سبب الرفض
        if (dto.getRejectionReason() != order.getRejectionReason()) {
            if (dto.getRejectionReason() != null) {
                changesLog.append(String.format("تعديل سبب الرفض إلى '%s'. ", dto.getRejectionReason().getArabicName()));
            } else if (order.getRejectionReason() != null) {
                changesLog.append("إزالة سبب الرفض. ");
            }
            order.setRejectionReason(dto.getRejectionReason());
        }

        // تحديث تفاصيل سبب الرفض
        String dtoNotes = dto.getRejectionReasonNotes();
        String orderNotes = order.getRejectionReasonNotes();
        if (dtoNotes != null && !dtoNotes.equals(orderNotes)) {
            order.setRejectionReasonNotes(dtoNotes);
        } else if (dtoNotes == null && orderNotes != null && dto.getRejectionReason() == null) {
            order.setRejectionReasonNotes(null);
        }

        Order savedOrder = orderRepository.save(order);

        if (changesLog.length() > 0) {
            orderStatusService.recordStatusChange(savedOrder, savedOrder.getStatus(), savedOrder.getStatus(), updatedBy,
                    oldAmount,
                    savedOrder.getAmount(),
                    changesLog.toString());
        }

        return savedOrder;
    }

    // حذف طلب بناءً على الصلاحيات وعدم التعارض مع القيود (يجب ألا يكون مسنداً أو في حالة نهائية)
    @Transactional
    public void deleteOrder(Long orderId, User user) {
        if (user.getRole() != com.shipment.shippinggo.enums.Role.ADMIN
                && user.getRole() != com.shipment.shippinggo.enums.Role.MANAGER) {
            throw new UnauthorizedAccessException("Unauthorized: Only Admins can delete orders");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!orderAssignmentService.canUserAccessOrder(user, order)) {
            throw new UnauthorizedAccessException("غير مصرح بالوصول لهذا الطلب: لا يمكنك حذفه");
        }

        if (order.getAssignedToOrganization() != null) {
            throw new BusinessLogicException("لا يمكن حذف طلب مسند لمنظمة أخرى ('"
                    + order.getAssignedToOrganization().getName() + "'). يجب إلغاء الإسناد أولاً.");
        }
        if (order.getAssignedToCourier() != null) {
            throw new BusinessLogicException("لا يمكن حذف طلب مسند لمندوب. يجب إلغاء إسناد المندوب أولاً.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.REFUSED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessLogicException("لا يمكن حذف طلب في حالة نهائية ("
                    + order.getStatus().getArabicName() + ").");
        }

        Organization userOrg = resolveUserOrganization(user);
        if (userOrg == null || !userOrg.getId().equals(order.getOwnerOrganization().getId())) {
            throw new UnauthorizedAccessException("غير مصرح: فقط المنظمة المالكة يمكنها حذف طلباتها.");
        }

        orderStatusHistoryRepository.deleteByOrder(order);
        orderAssignmentRepository.deleteByOrderId(orderId);
        orderEventRepository.deleteByOrderId(orderId);
        orderInquiryRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);
    }

    // حذف جميع الطلبات غير المسندة وغير النهائية المرتبطة بيوم عمل معين (يستخدم عند حذف يوم عمل)
    @Transactional
    public void deleteOrdersByBusinessDay(Long businessDayId, User user) {
        if (user.getRole() != com.shipment.shippinggo.enums.Role.ADMIN
                && user.getRole() != com.shipment.shippinggo.enums.Role.MANAGER) {
            throw new UnauthorizedAccessException("غير مصرح: المشرف أو المدير فقط يمكنه حذف الطلبات");
        }

        BusinessDay bd = businessDayRepository.findById(businessDayId)
                .orElseThrow(() -> new ResourceNotFoundException("يوم العمل غير موجود"));

        Organization userOrg = resolveUserOrganization(user);

        if (userOrg == null) {
            throw new UnauthorizedAccessException("المستخدم غير مرتبط بمنظمة");
        }

        if (!bd.getOrganization().getId().equals(userOrg.getId())) {
            throw new UnauthorizedAccessException("غير مصرح: يوم العمل لا يتبع لمنظمتك");
        }

        java.util.List<Order> orders = orderRepository.findByBusinessDayId(businessDayId);

        for (Order order : orders) {
            boolean isSystemOrder = order.getOwnerOrganization().getId().equals(userOrg.getId());
            boolean isUnassigned = order.getAssignedToCourier() == null && order.getAssignedToOrganization() == null;
            boolean isTerminalStatus = order.getStatus() == OrderStatus.DELIVERED ||
                    order.getStatus() == OrderStatus.REFUSED ||
                    order.getStatus() == OrderStatus.CANCELLED;

            if (isSystemOrder && isUnassigned && !isTerminalStatus) {
                orderStatusHistoryRepository.deleteByOrder(order);
                orderAssignmentRepository.deleteByOrderId(order.getId());
                orderEventRepository.deleteByOrderId(order.getId());
                orderInquiryRepository.deleteByOrderId(order.getId());
                orderRepository.delete(order);
            }
        }
    }

    // === Helper methods / دوال مساعدة ===

    // الحصول على المنظمة التي ينتمي إليها المستخدم (الشركة، المكتب، إلخ) أو عضوياته
    /**
     * @deprecated استخدم {@link OrganizationResolverService#resolveUserOrganization(User)} بدلاً.
     * تم الإبقاء عليها للتوافقية — تُفوّض للـ service الجديد.
     */
    @Deprecated
    Organization resolveUserOrganization(User user) {
        return organizationResolverService.resolveUserOrganization(user);
    }

    private String getDistrictFallback(String center, String area) {
        if (area != null && !area.trim().isEmpty()) {
            return area;
        }
        if (center != null && !center.trim().isEmpty()) {
            return center;
        }
        return null;
    }
}
