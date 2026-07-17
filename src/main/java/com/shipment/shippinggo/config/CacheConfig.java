package com.shipment.shippinggo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String REPORTS_CACHE = "reportsCache";
    public static final String DASHBOARDS_CACHE = "dashboards";
    public static final String USER_ORGANIZATIONS_CACHE = "userOrganizations";
    public static final String MEMBERSHIP_CHECKS_CACHE = "membershipChecks";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                REPORTS_CACHE, DASHBOARDS_CACHE, USER_ORGANIZATIONS_CACHE, MEMBERSHIP_CHECKS_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
