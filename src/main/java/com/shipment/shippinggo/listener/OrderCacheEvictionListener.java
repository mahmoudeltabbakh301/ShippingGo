package com.shipment.shippinggo.listener;

import com.shipment.shippinggo.entity.Order;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

@Component
public class OrderCacheEvictionListener implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    @PostPersist
    @PostUpdate
    @PostRemove
    public void onEntityChange(Object entity) {
        if (context != null) {
            CacheManager cacheManager = context.getBean(CacheManager.class);
            if (cacheManager != null) {
                org.springframework.cache.Cache cache = cacheManager.getCache("dashboards");
                if (cache != null) {
                    cache.clear();
                }
            }
        }
    }
}
