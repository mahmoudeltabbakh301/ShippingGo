package com.shipment.shippinggo.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Store;
import com.shipment.shippinggo.entity.StoreIntegration;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.repository.StoreIntegrationRepository;

@Service
public class StoreIntegrationService {

    private final StoreIntegrationRepository storeIntegrationRepository;

    public StoreIntegrationService(StoreIntegrationRepository storeIntegrationRepository) {
        this.storeIntegrationRepository = storeIntegrationRepository;
    }

    /**
     * إنشاء ربط جديد مع منصة خارجية لمتجر وتوليد API Key فريد
     */
    @Transactional
    public StoreIntegration createIntegration(Store store, IntegrationPlatform platform, String externalStoreUrl) {
        if (storeIntegrationRepository.existsByStoreIdAndPlatform(store.getId(), platform)) {
            throw new DuplicateResourceException("يوجد ربط مسبق مع منصة " + platform.getDisplayName() + ". يمكنك تعديله أو حذفه.");
        }

        String apiKey = generateUniqueApiKey();

        StoreIntegration integration = StoreIntegration.builder()
                .store(store)
                .platform(platform)
                .webhookApiKey(apiKey)
                .externalStoreUrl(externalStoreUrl)
                .active(true)
                .build();

        return storeIntegrationRepository.save(integration);
    }

    /**
     * إنشاء ربط جديد مع نظام خارجي لمنظمة (شركة أو مكتب)
     */
    @Transactional
    public StoreIntegration createOrganizationIntegration(Organization organization, IntegrationPlatform platform,
            String callbackUrl, String externalStoreUrl) {
        if (storeIntegrationRepository.existsByOrganizationIdAndPlatform(organization.getId(), platform)) {
            throw new DuplicateResourceException("يوجد ربط مسبق مع منصة " + platform.getDisplayName() + ". يمكنك تعديله أو حذفه.");
        }

        String apiKey = generateUniqueApiKey();
        String callbackSecret = callbackUrl != null ? generateUniqueApiKey() : null;

        StoreIntegration integration = StoreIntegration.builder()
                .organization(organization)
                .platform(platform)
                .webhookApiKey(apiKey)
                .externalStoreUrl(externalStoreUrl)
                .callbackUrl(callbackUrl)
                .callbackSecret(callbackSecret)
                .active(true)
                .build();

        return storeIntegrationRepository.save(integration);
    }

    /**
     * التحقق من صحة الـ API Key وإرجاع الربط المقابل
     */
    public Optional<StoreIntegration> validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        return storeIntegrationRepository.findByWebhookApiKeyAndActiveTrue(apiKey);
    }

    /**
     * جلب جميع عمليات الربط لمتجر معين
     */
    public List<StoreIntegration> getIntegrationsByStore(Long storeId) {
        return storeIntegrationRepository.findByStoreId(storeId);
    }

    /**
     * جلب جميع عمليات الربط لمنظمة معينة (شركة أو مكتب)
     */
    public List<StoreIntegration> getIntegrationsByOrganization(Long organizationId) {
        return storeIntegrationRepository.findByOrganizationId(organizationId);
    }

    /**
     * جلب الربطات التي لها Callback URL لإرسال تحديثات الحالات
     */
    public List<StoreIntegration> getActiveIntegrationsWithCallback(Long orgId) {
        return storeIntegrationRepository.findActiveIntegrationsWithCallbackByOrgId(orgId);
    }

    /**
     * تفعيل / تعطيل الربط
     */
    @Transactional
    public void toggleIntegration(Long integrationId) {
        StoreIntegration integration = storeIntegrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("الربط غير موجود"));

        integration.setActive(!integration.isActive());
        storeIntegrationRepository.save(integration);
    }

    /**
     * حذف الربط
     */
    @Transactional
    public void deleteIntegration(Long integrationId) {
        StoreIntegration integration = storeIntegrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("الربط غير موجود"));

        storeIntegrationRepository.delete(integration);
    }

    /**
     * إعادة توليد API Key جديد للربط
     */
    @Transactional
    public StoreIntegration regenerateApiKey(Long integrationId) {
        StoreIntegration integration = storeIntegrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("الربط غير موجود"));

        integration.setWebhookApiKey(generateUniqueApiKey());
        return storeIntegrationRepository.save(integration);
    }

    /**
     * تحديث رابط الـ Callback
     */
    @Transactional
    public StoreIntegration updateCallbackUrl(Long integrationId, String callbackUrl) {
        StoreIntegration integration = storeIntegrationRepository.findById(integrationId)
                .orElseThrow(() -> new ResourceNotFoundException("الربط غير موجود"));

        integration.setCallbackUrl(callbackUrl);
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            if (integration.getCallbackSecret() == null) {
                integration.setCallbackSecret(generateUniqueApiKey());
            }
        }
        return storeIntegrationRepository.save(integration);
    }

    /**
     * تحديث وقت آخر Webhook وصل
     */
    @Transactional
    public void updateLastWebhookReceived(Long integrationId) {
        StoreIntegration integration = storeIntegrationRepository.findById(integrationId).orElse(null);
        if (integration != null) {
            integration.setLastWebhookReceivedAt(LocalDateTime.now());
            storeIntegrationRepository.save(integration);
        }
    }

    public StoreIntegration getById(Long id) {
        return storeIntegrationRepository.findById(id).orElse(null);
    }

    /**
     * الحصول على المنظمة المرتبطة بالربط (Store أو Organization)
     */
    public Organization getIntegrationOwner(StoreIntegration integration) {
        if (integration.getOrganization() != null) {
            return integration.getOrganization();
        }
        return integration.getStore();
    }

    /**
     * توليد UUID فريد كـ API Key
     */
    private String generateUniqueApiKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
