package com.shipment.shippinggo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * كل ساعة: تعليق الاشتراكات المنتهية (تجريبية + نشطة)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void suspendExpiredSubscriptions() {
        try {
            int expiredTrials = subscriptionService.suspendExpiredTrials();
            int expiredActive = subscriptionService.suspendExpiredActive();

            if (expiredTrials > 0 || expiredActive > 0) {
                System.out.println(String.format(
                        "[SubscriptionScheduler] Suspended %d expired trials, %d expired active subscriptions.",
                        expiredTrials, expiredActive));
            }
        } catch (Exception e) {
            System.err.println("[SubscriptionScheduler] Error suspending expired subscriptions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * كل يوم الساعة 9 صباحاً: تنبيه الاشتراكات التي ستنتهي خلال 3 أيام
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyExpiringSubscriptions() {
        try {
            int notified = subscriptionService.notifyExpiringSubscriptions(3);
            if (notified > 0) {
                System.out.println(String.format(
                        "[SubscriptionScheduler] Sent %d expiration warnings.", notified));
            }
        } catch (Exception e) {
            System.err.println("[SubscriptionScheduler] Error sending expiry notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * كل يوم الساعة 6 مساءً: تنبيه ثانٍ للاشتراكات التي ستنتهي غداً
     */
    @Scheduled(cron = "0 0 18 * * *")
    public void notifyLastDaySubscriptions() {
        try {
            int notified = subscriptionService.notifyExpiringSubscriptions(1);
            if (notified > 0) {
                System.out.println(String.format(
                        "[SubscriptionScheduler] Sent %d last-day warnings.", notified));
            }
        } catch (Exception e) {
            System.err.println("[SubscriptionScheduler] Error sending last-day notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
