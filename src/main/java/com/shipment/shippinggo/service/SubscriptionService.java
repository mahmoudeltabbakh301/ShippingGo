package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.enums.SubscriptionStatus;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformSettingService platformSettingService;
    private final NotificationService notificationService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                OrganizationRepository organizationRepository,
                                PlatformSettingService platformSettingService,
                                NotificationService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.platformSettingService = platformSettingService;
        this.notificationService = notificationService;
    }

    // ===== Subscription Lifecycle =====

    /**
     * إنشاء اشتراك تجريبي جديد عند تسجيل منظمة
     */
    @Transactional
    public Subscription createTrialSubscription(Organization organization) {
        // استثناء المكاتب الافتراضية والعملاء من نظام الاشتراكات
        if (organization.getType() == com.shipment.shippinggo.enums.OrganizationType.VIRTUAL_OFFICE ||
            organization.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT) {
            return null;
        }

        // لا ننشئ اشتراك إذا كان موجوداً بالفعل
        if (subscriptionRepository.existsByOrganizationId(organization.getId())) {
            return subscriptionRepository.findByOrganizationId(organization.getId()).orElse(null);
        }

        int trialDays = platformSettingService.getDefaultTrialDays();
        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .organization(organization)
                .status(SubscriptionStatus.TRIAL)
                .monthlyPrice(platformSettingService.getDefaultMonthlyPrice())
                .trialStartDate(now)
                .trialEndDate(now.plusDays(trialDays))
                .autoRenew(true)
                .build();

        return subscriptionRepository.save(subscription);
    }

    /**
     * تفعيل الاشتراك بعد الدفع الناجح
     */
    @Transactional
    public Subscription activateSubscription(Long organizationId, int periodMonths) {
        Subscription subscription = getSubscriptionByOrgId(organizationId);
        LocalDateTime now = LocalDateTime.now();

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusMonths(periodMonths));

        // تفعيل المنظمة
        Organization org = subscription.getOrganization();
        if (!org.isActive()) {
            org.setActive(true);
            organizationRepository.save(org);
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * تعليق الاشتراك يدوياً من السوبر أدمن
     */
    @Transactional
    public Subscription suspendSubscription(Long organizationId, String notes) {
        Subscription subscription = getSubscriptionByOrgId(organizationId);
        subscription.setStatus(SubscriptionStatus.SUSPENDED);
        if (notes != null && !notes.isEmpty()) {
            subscription.setNotes(notes);
        }

        // تعطيل المنظمة
        Organization org = subscription.getOrganization();
        org.setActive(false);
        organizationRepository.save(org);

        return subscriptionRepository.save(subscription);
    }

    /**
     * تفعيل يدوي من السوبر أدمن (بدون دفع)
     */
    @Transactional
    public Subscription manualActivation(Long organizationId, int periodMonths, String notes) {
        Subscription subscription = getSubscriptionByOrgId(organizationId);
        LocalDateTime now = LocalDateTime.now();

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStart(now);
        subscription.setCurrentPeriodEnd(now.plusMonths(periodMonths));
        if (notes != null && !notes.isEmpty()) {
            subscription.setNotes(notes);
        }

        // تفعيل المنظمة
        Organization org = subscription.getOrganization();
        if (!org.isActive()) {
            org.setActive(true);
            organizationRepository.save(org);
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * تحديد سعر الاشتراك لمنظمة معينة
     */
    @Transactional
    public Subscription setOrganizationPrice(Long organizationId, BigDecimal monthlyPrice) {
        Subscription subscription = getSubscriptionByOrgId(organizationId);
        subscription.setMonthlyPrice(monthlyPrice);
        return subscriptionRepository.save(subscription);
    }

    // ===== Scheduled Operations =====

    /**
     * تعليق الاشتراكات التجريبية المنتهية
     */
    @Transactional
    public int suspendExpiredTrials() {
        List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(LocalDateTime.now());
        int count = 0;

        for (Subscription sub : expiredTrials) {
            sub.setStatus(SubscriptionStatus.EXPIRED);

            // تعطيل المنظمة
            Organization org = sub.getOrganization();
            org.setActive(false);
            organizationRepository.save(org);

            subscriptionRepository.save(sub);

            // إرسال إشعار
            try {
                if (org.getAdmin() != null) {
                    notificationService.sendSystemNotification(
                            org.getAdmin(),
                            "انتهت الفترة التجريبية",
                            "انتهت الفترة التجريبية المجانية لمنظمتك \"" + org.getName() + "\". يرجى الاشتراك لمتابعة استخدام المنصة."
                    );
                }
            } catch (Exception e) {
                // تسجيل الخطأ لكن لا نوقف العملية
                e.printStackTrace();
            }

            count++;
        }

        return count;
    }

    /**
     * تعليق الاشتراكات النشطة المنتهية
     */
    @Transactional
    public int suspendExpiredActive() {
        List<Subscription> expiredActive = subscriptionRepository.findExpiredActive(LocalDateTime.now());
        int count = 0;

        for (Subscription sub : expiredActive) {
            sub.setStatus(SubscriptionStatus.EXPIRED);

            Organization org = sub.getOrganization();
            org.setActive(false);
            organizationRepository.save(org);

            subscriptionRepository.save(sub);

            try {
                if (org.getAdmin() != null) {
                    notificationService.sendSystemNotification(
                            org.getAdmin(),
                            "انتهى اشتراكك",
                            "انتهت فترة اشتراك منظمتك \"" + org.getName() + "\". يرجى التجديد لمتابعة استخدام المنصة."
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            count++;
        }

        return count;
    }

    /**
     * إرسال تنبيهات للاشتراكات التي ستنتهي قريباً
     */
    @Transactional
    public int notifyExpiringSubscriptions(int daysBeforeExpiry) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime to = now.plusDays(daysBeforeExpiry);
        int count = 0;

        // تنبيهات الفترة التجريبية
        List<Subscription> expiringTrials = subscriptionRepository.findExpiringTrials(now, to);
        for (Subscription sub : expiringTrials) {
            // منع التكرار: لا نرسل أكثر من مرة في اليوم
            if (sub.getLastNotificationSentAt() != null &&
                    sub.getLastNotificationSentAt().toLocalDate().equals(now.toLocalDate())) {
                continue;
            }

            try {
                Organization org = sub.getOrganization();
                long remainingDays = sub.getRemainingDays();

                if (org.getAdmin() != null) {
                    notificationService.sendSystemNotification(
                            org.getAdmin(),
                            "فترتك التجريبية تنتهي قريباً",
                            "تبقى " + remainingDays + " يوم على انتهاء الفترة التجريبية المجانية لمنظمتك \"" + org.getName() + "\". اشترك الآن لتجنب إيقاف الحساب."
                    );
                }

                sub.setLastNotificationSentAt(now);
                subscriptionRepository.save(sub);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // تنبيهات الاشتراكات النشطة
        List<Subscription> expiringActive = subscriptionRepository.findExpiringActive(now, to);
        for (Subscription sub : expiringActive) {
            if (sub.getLastNotificationSentAt() != null &&
                    sub.getLastNotificationSentAt().toLocalDate().equals(now.toLocalDate())) {
                continue;
            }

            try {
                Organization org = sub.getOrganization();
                long remainingDays = sub.getRemainingDays();

                if (org.getAdmin() != null) {
                    notificationService.sendSystemNotification(
                            org.getAdmin(),
                            "اشتراكك ينتهي قريباً",
                            "تبقى " + remainingDays + " يوم على انتهاء اشتراك منظمتك \"" + org.getName() + "\". جدد الآن لتجنب إيقاف الحساب."
                    );
                }

                sub.setLastNotificationSentAt(now);
                subscriptionRepository.save(sub);
                count++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return count;
    }

    // ===== Queries =====

    public Subscription getSubscriptionByOrgId(Long organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("لا يوجد اشتراك لهذه المنظمة"));
    }

    public Subscription getSubscriptionByOrgIdOrNull(Long organizationId) {
        return subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public List<Subscription> getSubscriptionsByStatus(SubscriptionStatus status) {
        return subscriptionRepository.findByStatus(status);
    }

    public boolean hasActiveSubscription(Long organizationId) {
        Subscription sub = subscriptionRepository.findByOrganizationId(organizationId).orElse(null);
        return sub != null && sub.isCurrentlyActive();
    }

    // ===== Stats for Super Admin =====

    public SubscriptionStats getSubscriptionStats() {
        long total = subscriptionRepository.count();
        long trial = subscriptionRepository.countByStatus(SubscriptionStatus.TRIAL);
        long active = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long expired = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);
        long suspended = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);
        long activeTrials = subscriptionRepository.countActiveTrials(LocalDateTime.now());

        return new SubscriptionStats(total, trial, active, expired, suspended, activeTrials);
    }

    public static class SubscriptionStats {
        private final long total;
        private final long trial;
        private final long active;
        private final long expired;
        private final long suspended;
        private final long activeTrials;

        public SubscriptionStats(long total, long trial, long active, long expired, long suspended, long activeTrials) {
            this.total = total;
            this.trial = trial;
            this.active = active;
            this.expired = expired;
            this.suspended = suspended;
            this.activeTrials = activeTrials;
        }

        public long getTotal() { return total; }
        public long getTrial() { return trial; }
        public long getActive() { return active; }
        public long getExpired() { return expired; }
        public long getSuspended() { return suspended; }
        public long getActiveTrials() { return activeTrials; }
    }
}
