package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.CourierZoneAssignmentRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * خدمة التوزيع التلقائي — توزيع الأوردرات المختارة على المناديب
 * بناءً على المحافظة والمنطقة المربوطة لكل مندوب.
 */
@Service
public class AutoDistributionService {

    private static final Logger log = LoggerFactory.getLogger(AutoDistributionService.class);

    private final CourierZoneAssignmentRepository zoneAssignmentRepository;
    private final OrderAssignmentService orderAssignmentService;
    private final OrderRepository orderRepository;
    private final ArabicTextMatcher arabicTextMatcher;

    public AutoDistributionService(CourierZoneAssignmentRepository zoneAssignmentRepository,
                                   @org.springframework.context.annotation.Lazy OrderAssignmentService orderAssignmentService,
                                   OrderRepository orderRepository,
                                   ArabicTextMatcher arabicTextMatcher) {
        this.zoneAssignmentRepository = zoneAssignmentRepository;
        this.orderAssignmentService = orderAssignmentService;
        this.orderRepository = orderRepository;
        this.arabicTextMatcher = arabicTextMatcher;
    }

    // ==================== إدارة ربط المناديب بالمناطق ====================

    public List<CourierZoneAssignment> getZoneAssignments(Long organizationId) {
        return zoneAssignmentRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    public List<CourierZoneAssignment> getCourierZones(Long courierId, Long organizationId) {
        return zoneAssignmentRepository.findByCourierIdAndOrganizationIdAndActiveTrue(courierId, organizationId);
    }

    @Transactional
    public CourierZoneAssignment assignCourierToZone(User courier, Organization org,
                                                     Governorate governorate, String district) {
        return assignCourierToZone(courier, org, governorate, district, null, null);
    }

    @Transactional
    public CourierZoneAssignment assignCourierToZone(User courier, Organization org,
                                                     Governorate governorate, String district,
                                                     String center, String area) {
        // التحقق من عدم التكرار (فقط بين النشطة)
        if (center != null) {
            boolean exists = zoneAssignmentRepository.existsByCourierIdAndOrganizationIdAndGovernorateAndCenterAndAreaAndActiveTrue(
                    courier.getId(), org.getId(), governorate, center, area);
            if (exists) {
                throw new RuntimeException("المندوب مرتبط بالفعل بهذه المنطقة");
            }
        } else {
            boolean exists = zoneAssignmentRepository.existsByCourierIdAndOrganizationIdAndGovernorateAndDistrictAndActiveTrue(
                    courier.getId(), org.getId(), governorate, district);
            if (exists) {
                throw new RuntimeException("المندوب مرتبط بالفعل بهذه المنطقة");
            }
        }

        CourierZoneAssignment assignment = CourierZoneAssignment.builder()
                .courier(courier)
                .organization(org)
                .governorate(governorate)
                .district(district)
                .center(center)
                .area(area)
                .active(true)
                .build();
        return zoneAssignmentRepository.save(assignment);
    }

    @Transactional
    public void removeZoneAssignment(Long assignmentId) {
        zoneAssignmentRepository.findById(assignmentId).ifPresent(a -> {
            a.setActive(false);
            zoneAssignmentRepository.save(a);
        });
    }

    // ==================== التوزيع التلقائي ====================

    /**
     * توزيع أوردرات مختارة على المناديب بناءً على قواعد المناطق.
     * يُسند فقط الأوردرات التي لها مندوب مطابق للمنطقة.
     *
     * @param orderIds قائمة الأوردرات المختارة
     * @param orgId المنظمة
     * @param executedBy المستخدم المنفذ
     * @return نتيجة التوزيع
     */
    public DistributionResult autoDistribute(List<Long> orderIds, Long orgId, User executedBy) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        List<CourierZoneAssignment> allZones = zoneAssignmentRepository.findByOrganizationIdAndActiveTrue(orgId);

        int assigned = 0;
        int skipped = 0;
        List<DistributionDetail> details = new ArrayList<>();

        for (Order order : orders) {
            // تخطي الأوردرات المسندة بالفعل لمندوب
            if (order.getAssignedToCourier() != null) {
                skipped++;
                details.add(new DistributionDetail(order, null, "مُسند بالفعل لمندوب"));
                continue;
            }

            // تخطي الحالات النهائية
            if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.PARTIAL_DELIVERY ||
                order.getStatus() == OrderStatus.REFUSED) {
                skipped++;
                details.add(new DistributionDetail(order, null, "حالة نهائية"));
                continue;
            }

            // البحث عن المندوب المناسب
            User matchedCourier = findMatchingCourier(order, allZones);

            if (matchedCourier != null) {
                try {
                    orderAssignmentService.assignToCourier(order.getId(), matchedCourier.getId(), executedBy);
                    assigned++;
                    details.add(new DistributionDetail(order, matchedCourier, "تم الإسناد"));
                } catch (Exception e) {
                    skipped++;
                    details.add(new DistributionDetail(order, null, "فشل: " + e.getMessage()));
                    log.warn("Auto-distribution failed for order {}: {}", order.getCode(), e.getMessage());
                }
            } else {
                skipped++;
                details.add(new DistributionDetail(order, null, "لا يوجد مندوب مطابق للمنطقة"));
            }
        }

        log.info("Auto-distribution completed for org {}: {} assigned, {} skipped", orgId, assigned, skipped);
        return new DistributionResult(assigned, skipped, details);
    }

    /**
     * البحث عن المندوب المناسب:
     * 1. أولاً: بحث بالمحافظة + المركز + المنطقة (exact match)
     * 2. ثانياً: بحث بالمحافظة + المركز فقط
     * 3. ثالثاً: بحث بالمحافظة + district (legacy)
     * 4. رابعاً: Smart Match — بحث ذكي في العنوان النصي (recipientAddress)
     *    عن أسماء المناطق/المراكز المربوطة للمناديب في نفس المحافظة.
     *    يعمل فقط لما المحافظة محددة والمركز/المنطقة فاضيين.
     *    الأولوية: area > center > district
     * 5. خامساً: بحث بالمحافظة فقط (fallback — مندوب مسؤول عن كل المحافظة)
     */
    private User findMatchingCourier(Order order, List<CourierZoneAssignment> allZones) {
        if (order.getGovernorate() == null) return null;

        Governorate gov = order.getGovernorate();
        String center = order.getCenter();
        String area = order.getArea();
        String district = order.getDistrict();

        // خطوة 1: بحث exact (محافظة + مركز + منطقة)
        if (center != null && !center.trim().isEmpty() && area != null && !area.trim().isEmpty()) {
            for (CourierZoneAssignment zone : allZones) {
                if (zone.getGovernorate() == gov &&
                    center.equals(zone.getCenter()) &&
                    area.equals(zone.getArea())) {
                    return zone.getCourier();
                }
            }
        }

        // خطوة 2: بحث (محافظة + مركز فقط)
        if (center != null && !center.trim().isEmpty()) {
            for (CourierZoneAssignment zone : allZones) {
                if (zone.getGovernorate() == gov &&
                    center.equals(zone.getCenter()) &&
                    zone.getArea() == null) {
                    return zone.getCourier();
                }
            }
        }

        // خطوة 3: بحث legacy (محافظة + district)
        if (district != null && !district.trim().isEmpty()) {
            for (CourierZoneAssignment zone : allZones) {
                if (zone.getGovernorate() == gov &&
                    district.equals(zone.getDistrict()) &&
                    zone.getCenter() == null) {
                    return zone.getCourier();
                }
            }
        }

        // خطوة 4: Smart Address Match — بحث ذكي في العنوان النصي مع دعم الأخطاء الإملائية
        // يعمل فقط لما: المحافظة محددة + المركز والمنطقة فاضيين + العنوان النصي موجود
        // بيستخدم ArabicTextMatcher للتعامل مع (ة/ه، ى/ي، أ/إ/آ، أل التعريف، أخطاء إملائية)
        if ((center == null || center.trim().isEmpty()) &&
            (area == null || area.trim().isEmpty()) &&
            order.getRecipientAddress() != null && !order.getRecipientAddress().trim().isEmpty()) {

            String address = order.getRecipientAddress().trim();
            User bestMatch = null;
            int bestPriority = 0;    // 3 = area, 2 = center, 1 = district
            double bestScore = 0.0;  // نسبة التشابه الأعلى

            for (CourierZoneAssignment zone : allZones) {
                if (zone.getGovernorate() != gov) continue;

                // أولوية 3 (أعلى): مطابقة اسم المنطقة (area) في العنوان
                if (zone.getArea() != null && !zone.getArea().trim().isEmpty()) {
                    ArabicTextMatcher.MatchResult result = arabicTextMatcher.matchZoneInAddress(address, zone.getArea());
                    if (result.isMatched() && (3 > bestPriority || (3 == bestPriority && result.getScore() > bestScore))) {
                        bestPriority = 3;
                        bestScore = result.getScore();
                        bestMatch = zone.getCourier();
                    }
                }

                // أولوية 2: مطابقة اسم المركز (center) في العنوان
                if (zone.getCenter() != null && !zone.getCenter().trim().isEmpty() && bestPriority < 3) {
                    ArabicTextMatcher.MatchResult result = arabicTextMatcher.matchZoneInAddress(address, zone.getCenter());
                    if (result.isMatched() && (2 > bestPriority || (2 == bestPriority && result.getScore() > bestScore))) {
                        bestPriority = 2;
                        bestScore = result.getScore();
                        bestMatch = zone.getCourier();
                    }
                }

                // أولوية 1 (أقل): مطابقة اسم المنطقة القديم (district) في العنوان
                if (zone.getDistrict() != null && !zone.getDistrict().trim().isEmpty() && bestPriority < 2) {
                    ArabicTextMatcher.MatchResult result = arabicTextMatcher.matchZoneInAddress(address, zone.getDistrict());
                    if (result.isMatched() && (1 > bestPriority || (1 == bestPriority && result.getScore() > bestScore))) {
                        bestPriority = 1;
                        bestScore = result.getScore();
                        bestMatch = zone.getCourier();
                    }
                }
            }

            if (bestMatch != null) {
                log.info("Smart address match for order [{}]: courier {} via {} (score={}%, priority={})",
                        order.getCode(), bestMatch.getId(),
                        bestPriority == 3 ? "area" : bestPriority == 2 ? "center" : "district",
                        Math.round(bestScore * 100), bestPriority);
                return bestMatch;
            }
        }

        // خطوة 5: fallback (محافظة فقط)
        for (CourierZoneAssignment zone : allZones) {
            if (zone.getGovernorate() == gov &&
                zone.getDistrict() == null && zone.getCenter() == null) {
                return zone.getCourier();
            }
        }

        return null;
    }

    /**
     * معاينة التوزيع بدون تنفيذ — لعرض النتائج قبل التأكيد.
     */
    public DistributionResult previewDistribution(List<Long> orderIds, Long orgId) {
        List<Order> orders = orderRepository.findAllById(orderIds);
        List<CourierZoneAssignment> allZones = zoneAssignmentRepository.findByOrganizationIdAndActiveTrue(orgId);

        int wouldAssign = 0;
        int wouldSkip = 0;
        List<DistributionDetail> details = new ArrayList<>();

        for (Order order : orders) {
            if (order.getAssignedToCourier() != null) {
                wouldSkip++;
                details.add(new DistributionDetail(order, null, "مُسند بالفعل لمندوب"));
                continue;
            }

            User matchedCourier = findMatchingCourier(order, allZones);
            if (matchedCourier != null) {
                wouldAssign++;
                details.add(new DistributionDetail(order, matchedCourier, "سيتم الإسناد"));
            } else {
                wouldSkip++;
                details.add(new DistributionDetail(order, null, "لا يوجد مندوب مطابق"));
            }
        }

        return new DistributionResult(wouldAssign, wouldSkip, details);
    }

    // ==================== DTOs ====================

    public static class DistributionResult {
        private final int assignedCount;
        private final int skippedCount;
        private final List<DistributionDetail> details;

        public DistributionResult(int assignedCount, int skippedCount, List<DistributionDetail> details) {
            this.assignedCount = assignedCount;
            this.skippedCount = skippedCount;
            this.details = details;
        }

        public int getAssignedCount() { return assignedCount; }
        public int getSkippedCount() { return skippedCount; }
        public List<DistributionDetail> getDetails() { return details; }
        public int getTotalCount() { return assignedCount + skippedCount; }
    }

    public static class DistributionDetail {
        private final Order order;
        private final User courier;
        private final String status;

        public DistributionDetail(Order order, User courier, String status) {
            this.order = order;
            this.courier = courier;
            this.status = status;
        }

        public Order getOrder() { return order; }
        public User getCourier() { return courier; }
        public String getStatus() { return status; }
    }
}
