package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.repository.OrderReportRepository;
import com.shipment.shippinggo.repository.TargetSettingRepository;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * خدمة إدارة التارجت (المكافآت) للمناديب والمنظمات.
 * التارجت شهري يبدأ من تاريخ إعداده، ويحتسب على المبالغ المسلمة فقط.
 */
@Service
@Transactional
public class TargetService {

    private final TargetSettingRepository targetSettingRepository;
    private final OrderRepository orderRepository;
    private final OrderReportRepository orderReportRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;

    public TargetService(TargetSettingRepository targetSettingRepository,
            OrderRepository orderRepository,
            OrderReportRepository orderReportRepository,
            VirtualOfficeRepository virtualOfficeRepository) {
        this.targetSettingRepository = targetSettingRepository;
        this.orderRepository = orderRepository;
        this.orderReportRepository = orderReportRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
    }

    private Organization resolveEffectiveOrganization(Organization org) {
        if (org != null && org.getType() == OrganizationType.VIRTUAL_OFFICE) {
            return virtualOfficeRepository.findById(org.getId())
                    .map(vo -> vo.getParentOrganization() != null ? vo.getParentOrganization() : org)
                    .orElse(org);
        }
        return org;
    }

    // --- CRUD Operations ---

    /**
     * حفظ أو تحديث تارجت لمنظمة مستهدفة
     */
    public TargetSetting saveOrganizationTarget(Organization sourceOrg, Organization targetOrg,
            BigDecimal targetAmount, CommissionType rewardType, BigDecimal rewardValue) {

        sourceOrg = resolveEffectiveOrganization(sourceOrg);
        targetOrg = resolveEffectiveOrganization(targetOrg);

        Optional<TargetSetting> existing = targetSettingRepository
                .findBySourceOrganizationAndTargetOrganization(sourceOrg, targetOrg);

        TargetSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setTargetAmount(targetAmount);
            setting.setRewardType(rewardType);
            setting.setRewardValue(rewardValue);
            setting.setActive(true);
            // يتم إعادة تعيين تاريخ البداية عند التحديث
            setting.setStartDate(LocalDateTime.now());
        } else {
            setting = TargetSetting.builder()
                    .sourceOrganization(sourceOrg)
                    .targetOrganization(targetOrg)
                    .targetAmount(targetAmount)
                    .rewardType(rewardType)
                    .rewardValue(rewardValue)
                    .startDate(LocalDateTime.now())
                    .build();
        }
        return targetSettingRepository.save(setting);
    }

    /**
     * حفظ أو تحديث تارجت لمندوب
     */
    public TargetSetting saveCourierTarget(Organization sourceOrg, User courier,
            BigDecimal targetAmount, CommissionType rewardType, BigDecimal rewardValue) {

        sourceOrg = resolveEffectiveOrganization(sourceOrg);

        Optional<TargetSetting> existing = targetSettingRepository
                .findBySourceOrganizationAndCourier(sourceOrg, courier);

        TargetSetting setting;
        if (existing.isPresent()) {
            setting = existing.get();
            setting.setTargetAmount(targetAmount);
            setting.setRewardType(rewardType);
            setting.setRewardValue(rewardValue);
            setting.setActive(true);
            setting.setStartDate(LocalDateTime.now());
        } else {
            setting = TargetSetting.builder()
                    .sourceOrganization(sourceOrg)
                    .courier(courier)
                    .targetAmount(targetAmount)
                    .rewardType(rewardType)
                    .rewardValue(rewardValue)
                    .startDate(LocalDateTime.now())
                    .build();
        }
        return targetSettingRepository.save(setting);
    }

    /**
     * جلب جميع إعدادات التارجت لمنظمة معينة
     */
    public List<TargetSetting> getTargetSettings(Organization organization) {
        return targetSettingRepository.findBySourceOrganization(organization);
    }

    /**
     * جلب إعداد تارجت بالمعرف
     */
    public Optional<TargetSetting> getTargetSettingById(Long id) {
        return targetSettingRepository.findById(id);
    }

    /**
     * حذف إعداد تارجت
     */
    public void deleteTargetSetting(Long id) {
        targetSettingRepository.deleteById(id);
    }

    // --- Target Calculation ---

    /**
     * حساب الفترة الشهرية الحالية بناءً على تاريخ بداية التارجت.
     * مثال: إذا بدأ التارجت في 15 يناير، الشهر الحالي يكون من 15 الشهر الحالي إلى 15 الشهر التالي.
     */
    public LocalDateTime[] getCurrentMonthPeriod(TargetSetting setting) {
        LocalDateTime startDate = setting.getStartDate();
        LocalDateTime now = LocalDateTime.now();

        // حساب بداية الشهر الحالي بناءً على يوم بداية التارجت
        int dayOfMonth = startDate.getDayOfMonth();
        LocalDateTime periodStart;

        if (now.getDayOfMonth() >= dayOfMonth) {
            // نحن بعد يوم البداية في هذا الشهر
            periodStart = now.withDayOfMonth(Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth()))
                    .withHour(startDate.getHour())
                    .withMinute(startDate.getMinute())
                    .withSecond(0)
                    .withNano(0);
        } else {
            // نحن قبل يوم البداية - الفترة بدأت من الشهر السابق
            LocalDateTime lastMonth = now.minusMonths(1);
            periodStart = lastMonth.withDayOfMonth(Math.min(dayOfMonth, lastMonth.toLocalDate().lengthOfMonth()))
                    .withHour(startDate.getHour())
                    .withMinute(startDate.getMinute())
                    .withSecond(0)
                    .withNano(0);
        }

        // إذا كانت الفترة المحسوبة قبل تاريخ بداية التارجت، نستخدم تاريخ البداية
        if (periodStart.isBefore(startDate)) {
            periodStart = startDate;
        }

        LocalDateTime periodEnd = periodStart.plusMonths(1);

        return new LocalDateTime[] { periodStart, periodEnd };
    }

    /**
     * حساب المبلغ المسلم للمندوب خلال فترة معينة
     */
    public BigDecimal getCourierDeliveredAmount(User courier, LocalDateTime from, LocalDateTime to) {
        BigDecimal deliveredAmount = orderReportRepository
                .sumDeliveredAmountByCourierAndDateRange(courier.getId(), from, to);
        return deliveredAmount != null ? deliveredAmount : BigDecimal.ZERO;
    }

    /**
     * جلب المبالغ المسلمة للمندوب مقسمة بالأيام خلال فترة معينة
     */
    public List<com.shipment.shippinggo.dto.DailyTargetStatsDto> getDailyCourierDeliveredAmount(User courier, LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderReportRepository.findDeliveredOrdersByCourierAndDateRange(courier.getId(), from, to);
        return groupOrdersByDate(orders, true);
    }

    /**
     * حساب المبلغ المسلم للمنظمة خلال فترة معينة
     * (الأوردرات المسندة لهذه المنظمة من المنظمة المصدر وتم تسليمها)
     */
    public BigDecimal getOrganizationDeliveredAmount(Organization sourceOrg, Organization targetOrg,
            LocalDateTime from, LocalDateTime to) {
        BigDecimal deliveredAmount = orderReportRepository
                .sumDeliveredAmountByAssignedOrgAndDateRange(targetOrg.getId(), from, to);
        return deliveredAmount != null ? deliveredAmount : BigDecimal.ZERO;
    }

    /**
     * جلب المبالغ المسلمة للمنظمة مقسمة بالأيام خلال فترة معينة
     */
    public List<com.shipment.shippinggo.dto.DailyTargetStatsDto> getDailyOrganizationDeliveredAmount(Organization sourceOrg, Organization targetOrg,
            LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderReportRepository.findDeliveredOrdersByAssignedOrgAndDateRange(targetOrg.getId(), from, to);
        return groupOrdersByDate(orders, false);
    }

    private List<com.shipment.shippinggo.dto.DailyTargetStatsDto> groupOrdersByDate(List<Order> orders, boolean isCourier) {
        java.util.Map<java.time.LocalDate, BigDecimal> dailySums = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        
        for (Order o : orders) {
            java.time.LocalDate date;
            if (isCourier) {
                date = o.getCourierAssignmentDate() != null ? o.getCourierAssignmentDate().toLocalDate() : null;
            } else {
                date = o.getAssignmentDate();
            }
            
            if (date != null) {
                BigDecimal amount = BigDecimal.ZERO;
                if (o.getStatus() == OrderStatus.DELIVERED) {
                    amount = o.getCollectedAmount() != null ? o.getCollectedAmount() : (o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
                } else if (o.getStatus() == OrderStatus.PARTIAL_DELIVERY) {
                    amount = o.getPartialDeliveryAmount() != null ? o.getPartialDeliveryAmount() : BigDecimal.ZERO;
                }
                
                dailySums.put(date, dailySums.getOrDefault(date, BigDecimal.ZERO).add(amount));
            }
        }
        
        return dailySums.entrySet().stream()
                .map(e -> new com.shipment.shippinggo.dto.DailyTargetStatsDto(e.getKey(), e.getValue()))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * حساب مكافأة التارجت عند التحقيق
     * المكافأة تحسب على المبلغ الكلي المسلم (المحدد + الزائد)
     */
    public BigDecimal calculateReward(TargetSetting setting, BigDecimal deliveredAmount) {
        if (setting == null || deliveredAmount == null) {
            return BigDecimal.ZERO;
        }

        // لم يتم تحقيق التارجت
        if (deliveredAmount.compareTo(setting.getTargetAmount()) < 0) {
            return BigDecimal.ZERO;
        }

        // تم تحقيق التارجت - حساب المكافأة على المبلغ الكلي
        if (setting.getRewardType() == CommissionType.FIXED) {
            return setting.getRewardValue();
        } else {
            // نسبة مئوية من المبلغ الكلي المسلم
            return deliveredAmount.multiply(setting.getRewardValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
    }

    /**
     * حساب نسبة التقدم نحو التارجت (0-100)
     */
    public int calculateProgress(BigDecimal deliveredAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (deliveredAmount == null) {
            return 0;
        }
        BigDecimal progress = deliveredAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 0, RoundingMode.HALF_UP);
        return Math.min(progress.intValue(), 100);
    }
}
