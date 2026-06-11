package com.shipment.shippinggo.config;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.enums.SubscriptionStatus;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.SubscriptionRepository;
import com.shipment.shippinggo.service.PlatformSettingService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * عند أول تشغيل بعد إضافة نظام الاشتراكات:
 * ينشئ اشتراك تجريبي 14 يوم لجميع المنظمات الموجودة التي لا تملك اشتراكاً بعد
 */
@Component
@Order(100) // يعمل بعد initialization الأخرى
public class SubscriptionDataMigration implements ApplicationRunner {

    private final OrganizationRepository organizationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlatformSettingService platformSettingService;

    public SubscriptionDataMigration(OrganizationRepository organizationRepository,
                                      SubscriptionRepository subscriptionRepository,
                                      PlatformSettingService platformSettingService) {
        this.organizationRepository = organizationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.platformSettingService = platformSettingService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Organization> allOrgs = organizationRepository.findAll();
        int created = 0;

        int trialDays = platformSettingService.getDefaultTrialDays();
        LocalDateTime now = LocalDateTime.now();

        for (Organization org : allOrgs) {
            // تخطي المنظمات التي لها اشتراك بالفعل
            if (subscriptionRepository.existsByOrganizationId(org.getId())) {
                continue;
            }

            Subscription subscription = Subscription.builder()
                    .organization(org)
                    .status(SubscriptionStatus.TRIAL)
                    .monthlyPrice(platformSettingService.getDefaultMonthlyPrice())
                    .trialStartDate(now)
                    .trialEndDate(now.plusDays(trialDays))
                    .autoRenew(true)
                    .notes("تم إنشاؤه تلقائياً - ترحيل بيانات")
                    .build();

            subscriptionRepository.save(subscription);
            created++;
        }

        if (created > 0) {
            System.out.println("[SubscriptionDataMigration] Created " + created + " trial subscriptions for existing organizations (trial: " + trialDays + " days).");
        }
    }
}
