package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.GovernorateRoutingRuleRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GovernorateRoutingService {

    private static final Logger log = LoggerFactory.getLogger(GovernorateRoutingService.class);

    private final GovernorateRoutingRuleRepository routingRuleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrderAssignmentService orderAssignmentService;

    public GovernorateRoutingService(GovernorateRoutingRuleRepository routingRuleRepository,
            OrganizationRepository organizationRepository,
            @org.springframework.context.annotation.Lazy OrderAssignmentService orderAssignmentService) {
        this.routingRuleRepository = routingRuleRepository;
        this.organizationRepository = organizationRepository;
        this.orderAssignmentService = orderAssignmentService;
    }

    /**
     * حفظ / تعديل قاعدة توزيع لمحافظة معينة
     */
    @Transactional
    public GovernorateRoutingRule saveRule(Store store, Governorate governorate, Long targetOrganizationId) {
        Organization targetOrg = organizationRepository.findById(targetOrganizationId)
                .orElseThrow(() -> new ResourceNotFoundException("المنظمة المستهدفة غير موجودة"));

        // البحث عن قاعدة موجودة لهذا المتجر والمحافظة
        Optional<GovernorateRoutingRule> existingOpt = routingRuleRepository
                .findByStoreIdAndGovernorateAndActiveTrue(store.getId(), governorate);

        if (existingOpt.isPresent()) {
            // تعديل القاعدة الموجودة
            GovernorateRoutingRule existing = existingOpt.get();
            existing.setTargetOrganization(targetOrg);
            return routingRuleRepository.save(existing);
        }

        // إنشاء قاعدة جديدة
        GovernorateRoutingRule rule = GovernorateRoutingRule.builder()
                .store(store)
                .governorate(governorate)
                .targetOrganization(targetOrg)
                .active(true)
                .build();

        return routingRuleRepository.save(rule);
    }

    /**
     * حذف قاعدة توزيع
     */
    @Transactional
    public void deleteRule(Long ruleId) {
        GovernorateRoutingRule rule = routingRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("القاعدة غير موجودة"));
        routingRuleRepository.delete(rule);
    }

    /**
     * جلب جميع قواعد التوزيع لمتجر معين
     */
    public List<GovernorateRoutingRule> getRulesByStore(Long storeId) {
        return routingRuleRepository.findByStoreIdOrderByGovernorate(storeId);
    }

    /**
     * محاولة التوزيع التلقائي للأوردر بناءً على المحافظة.
     * إذا وُجدت قاعدة مطابقة → يتم الإسناد تلقائياً لشركة الشحن.
     * إذا لم تُوجد → الأوردر يبقى بدون تعيين (يُعين يدوياً).
     */
    @Transactional
    public boolean autoAssignIfRuleExists(Order order, Store store) {
        if (order.getGovernorate() == null) {
            log.info("الأوردر {} ليس له محافظة محددة - سيتم تعيينه يدوياً", order.getCode());
            return false;
        }

        Optional<GovernorateRoutingRule> ruleOpt = routingRuleRepository
                .findByStoreIdAndGovernorateAndActiveTrue(store.getId(), order.getGovernorate());

        if (ruleOpt.isEmpty()) {
            log.info("لا توجد قاعدة توزيع للمحافظة {} في المتجر {} - سيتم تعيينه يدوياً",
                    order.getGovernorate().getArabicName(), store.getName());
            return false;
        }

        GovernorateRoutingRule rule = ruleOpt.get();
        Organization targetOrg = rule.getTargetOrganization();

        try {
            orderAssignmentService.assignToOrganization(order.getId(), targetOrg, store.getAdmin());
            log.info("تم توزيع الأوردر {} تلقائياً إلى '{}' (محافظة: {})",
                    order.getCode(), targetOrg.getName(), order.getGovernorate().getArabicName());
            return true;
        } catch (Exception e) {
            log.warn("فشل التوزيع التلقائي للأوردر {} إلى '{}': {}",
                    order.getCode(), targetOrg.getName(), e.getMessage());
            return false;
        }
    }
}
