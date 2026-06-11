package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.PlatformSetting;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.PlatformSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PlatformSettingService {

    private final PlatformSettingRepository settingRepository;

    public PlatformSettingService(PlatformSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * إنشاء الإعدادات الافتراضية عند أول تشغيل
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void initializeDefaultSettings() {
        // إعدادات الاشتراك
        createIfNotExists("default_trial_days", "14", "عدد أيام الفترة التجريبية المجانية");
        createIfNotExists("default_monthly_price", "500", "سعر الاشتراك الشهري الافتراضي (جنيه)");

        // إعدادات Paymob
        createIfNotExists("paymob_api_key", "", "مفتاح API لـ Paymob");
        createIfNotExists("paymob_secret_key", "", "المفتاح السري لـ Paymob");
        createIfNotExists("paymob_integration_id", "", "Integration ID لـ Paymob (Card)");
        createIfNotExists("paymob_hmac_secret", "", "HMAC Secret للتحقق من Webhooks");

        // إعدادات المنصة العامة
        createIfNotExists("platform_name", "ShippingGo", "اسم المنصة");
        createIfNotExists("platform_currency", "EGP", "عملة المنصة");
        createIfNotExists("platform_logo_url", "", "رابط شعار المنصة");
        createIfNotExists("platform_contact_email", "", "البريد الإلكتروني للتواصل");
        createIfNotExists("platform_contact_phone", "", "رقم الهاتف للتواصل");
        createIfNotExists("platform_whatsapp", "", "رقم الواتساب");
        createIfNotExists("platform_address", "", "عنوان المنصة");

        // إعدادات الإشعارات
        createIfNotExists("notification_firebase_enabled", "true", "تفعيل إشعارات Firebase");

        // صفحات ثابتة
        createIfNotExists("terms_and_conditions", "", "الشروط والأحكام");
        createIfNotExists("privacy_policy", "", "سياسة الخصوصية");

        // وضع الصيانة
        createIfNotExists("maintenance_mode", "false", "وضع الصيانة (true/false)");
        createIfNotExists("maintenance_message", "المنصة تحت الصيانة حالياً. سنعود قريباً.", "رسالة الصيانة");
    }

    private void createIfNotExists(String key, String value, String description) {
        if (!settingRepository.existsBySettingKey(key)) {
            PlatformSetting setting = PlatformSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .description(description)
                    .build();
            settingRepository.save(setting);
        }
    }

    // ===== CRUD =====

    public String getSetting(String key) {
        return settingRepository.findBySettingKey(key)
                .map(PlatformSetting::getSettingValue)
                .orElse(null);
    }

    public String getSetting(String key, String defaultValue) {
        String value = getSetting(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    @Transactional
    public void setSetting(String key, String value, User updatedBy) {
        PlatformSetting setting = settingRepository.findBySettingKey(key).orElse(null);
        if (setting != null) {
            setting.setSettingValue(value);
            setting.setUpdatedBy(updatedBy);
            settingRepository.save(setting);
        } else {
            setting = PlatformSetting.builder()
                    .settingKey(key)
                    .settingValue(value)
                    .updatedBy(updatedBy)
                    .build();
            settingRepository.save(setting);
        }
    }

    public List<PlatformSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    // ===== Convenience Methods =====

    public int getDefaultTrialDays() {
        try {
            return Integer.parseInt(getSetting("default_trial_days", "14"));
        } catch (NumberFormatException e) {
            return 14;
        }
    }

    public BigDecimal getDefaultMonthlyPrice() {
        try {
            return new BigDecimal(getSetting("default_monthly_price", "500"));
        } catch (NumberFormatException e) {
            return new BigDecimal("500");
        }
    }

    public String getPaymobApiKey() {
        return getSetting("paymob_api_key", "");
    }

    public String getPaymobSecretKey() {
        return getSetting("paymob_secret_key", "");
    }

    public String getPaymobIntegrationId() {
        return getSetting("paymob_integration_id", "");
    }

    public String getPaymobHmacSecret() {
        return getSetting("paymob_hmac_secret", "");
    }

    public String getPlatformCurrency() {
        return getSetting("platform_currency", "EGP");
    }
}
