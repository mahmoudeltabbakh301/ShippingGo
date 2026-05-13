package com.shipment.shippinggo.repository;

import com.shipment.shippinggo.entity.StoreIntegration;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreIntegrationRepository extends JpaRepository<StoreIntegration, Long> {

    Optional<StoreIntegration> findByWebhookApiKeyAndActiveTrue(String webhookApiKey);

    List<StoreIntegration> findByStoreId(Long storeId);

    Optional<StoreIntegration> findByStoreIdAndPlatform(Long storeId, IntegrationPlatform platform);

    boolean existsByStoreIdAndPlatform(Long storeId, IntegrationPlatform platform);

    // === Organization-based queries (for Company/Office integrations) ===

    List<StoreIntegration> findByOrganizationId(Long organizationId);

    Optional<StoreIntegration> findByOrganizationIdAndPlatform(Long organizationId, IntegrationPlatform platform);

    boolean existsByOrganizationIdAndPlatform(Long organizationId, IntegrationPlatform platform);

    // جلب جميع الربطات التي لها Callback URL (للإرسال عند تغيير الحالة)
    @Query("SELECT si FROM StoreIntegration si WHERE si.callbackUrl IS NOT NULL AND si.active = true AND (si.store.id = :orgId OR si.organization.id = :orgId)")
    List<StoreIntegration> findActiveIntegrationsWithCallbackByOrgId(@Param("orgId") Long orgId);
}
