package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.repository.*;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderInquiryService {

    private final OrderInquiryRepository orderInquiryRepository;
    private final OrderRepository orderRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationService organizationService;
    private final BusinessDayService businessDayService;
    private final NotificationService notificationService;
    private final OrderAssignmentService orderAssignmentService;

    public OrderInquiryService(OrderInquiryRepository orderInquiryRepository,
            OrderRepository orderRepository,
            OrganizationRepository organizationRepository,
            OrganizationService organizationService,
            @org.springframework.context.annotation.Lazy BusinessDayService businessDayService,
            NotificationService notificationService,
            @org.springframework.context.annotation.Lazy OrderAssignmentService orderAssignmentService) {
        this.orderInquiryRepository = orderInquiryRepository;
        this.orderRepository = orderRepository;
        this.organizationRepository = organizationRepository;
        this.organizationService = organizationService;
        this.businessDayService = businessDayService;
        this.notificationService = notificationService;
        this.orderAssignmentService = orderAssignmentService;
    }

    /**
     * إرسال أوردرات كاستعلام لمنظمات محددة
     */
    @Transactional
    public int sendInquiry(List<Long> orderIds, List<Long> targetOrgIds, User sender) {
        Organization senderOrg = orderAssignmentService.resolveUserOrganization(sender);
        if (senderOrg == null) {
            throw new UnauthorizedAccessException("المستخدم غير مرتبط بأي منظمة.");
        }

        // فقط الشركات يمكنها إرسال الاستعلامات
        if (senderOrg.getType() != OrganizationType.COMPANY) {
            throw new BusinessLogicException("الاستعلامات متاحة للشركات فقط.");
        }

        List<Organization> linkedOrgs = organizationService.getLinkedOrganizations(senderOrg);
        int sentCount = 0;

        for (Long targetOrgId : targetOrgIds) {
            Organization targetOrg = organizationRepository.findById(targetOrgId).orElse(null);
            if (targetOrg == null)
                continue;

            // التحقق من أن المنظمة مشتركة/مرتبطة
            boolean isLinked = linkedOrgs.stream()
                    .anyMatch(o -> o.getId().equals(targetOrgId));
            if (!isLinked) {
                continue;
            }

            // إنشاء يوم عمل للمنظمة المستلمة إذا لم يكن موجوداً
            BusinessDay receiverBd = businessDayService.ensureNormalBusinessDayExists(
                    targetOrgId, java.time.LocalDate.now(), sender);

            for (Long orderId : orderIds) {
                // تخطي إذا كان الاستعلام موجوداً مسبقاً
                if (orderInquiryRepository.existsByOrderIdAndReceiverOrganizationId(orderId, targetOrgId)) {
                    continue;
                }

                Order order = orderRepository.findById(orderId).orElse(null);
                if (order == null)
                    continue;

                OrderInquiry inquiry = OrderInquiry.builder()
                        .order(order)
                        .senderOrganization(senderOrg)
                        .receiverOrganization(targetOrg)
                        .businessDay(receiverBd)
                        .sentBy(sender)
                        .build();

                orderInquiryRepository.save(inquiry);
                sentCount++;
            }

            // إرسال إشعار للمنظمة المستلمة
            if (!orderIds.isEmpty()) {
                notificationService.sendNotificationToOrganization(targetOrg, "استعلامات جديدة",
                        "تم إرسال " + orderIds.size() + " أوردر كاستعلام من " + senderOrg.getName(),
                        java.util.Map.of("type", "ORDER_INQUIRY", "count", String.valueOf(orderIds.size())),
                        "ORDER_INQUIRY");
            }
        }

        return sentCount;
    }

    /**
     * إرسال للكل - لجميع المنظمات المشتركة
     */
    @Transactional
    public int sendInquiryToAll(List<Long> orderIds, User sender) {
        Organization senderOrg = orderAssignmentService.resolveUserOrganization(sender);
        if (senderOrg == null) {
            throw new UnauthorizedAccessException("المستخدم غير مرتبط بأي منظمة.");
        }

        if (senderOrg.getType() != OrganizationType.COMPANY) {
            throw new BusinessLogicException("الاستعلامات متاحة للشركات فقط.");
        }

        List<Office> linkedOffices = organizationService.getOfficesByCompany(senderOrg.getId());
        List<Long> targetOrgIds = linkedOffices.stream().map(Office::getId).toList();

        return sendInquiry(orderIds, targetOrgIds, sender);
    }

    /**
     * جلب الاستعلامات الواردة لمنظمة ويوم عمل مع بيانات الأوردرات
     */
    public List<OrderInquiry> getInquiriesForBusinessDay(Long orgId, Long bdId) {
        return orderInquiryRepository.findByReceiverOrganizationIdAndBusinessDayId(orgId, bdId);
    }

    /**
     * عدد الاستعلامات الواردة ليوم عمل
     */
    public long getInquiryCount(Long orgId, Long bdId) {
        return orderInquiryRepository.countByReceiverOrganizationIdAndBusinessDayId(orgId, bdId);
    }

    /**
     * إزالة استعلام (المنظمة المُرسِلة فقط)
     */
    @Transactional
    public void removeInquiry(Long inquiryId, User user) {
        OrderInquiry inquiry = orderInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("الاستعلام غير موجود"));

        Organization userOrg = orderAssignmentService.resolveUserOrganization(user);
        if (userOrg == null || !userOrg.getId().equals(inquiry.getSenderOrganization().getId())) {
            throw new UnauthorizedAccessException("فقط المنظمة المُرسِلة يمكنها إزالة الاستعلام.");
        }

        orderInquiryRepository.delete(inquiry);
    }

    /**
     * حذف جميع استعلامات أوردر عند حذف الأوردر
     */
    @Transactional
    public void deleteByOrderId(Long orderId) {
        orderInquiryRepository.deleteByOrderId(orderId);
    }
}
