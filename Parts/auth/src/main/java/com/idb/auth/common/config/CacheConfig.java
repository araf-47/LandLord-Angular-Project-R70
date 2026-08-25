package com.idb.auth.common.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.CollectionUtils;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.idb.auth.common.dto.CacheConfigParams;
import com.idb.auth.common.util.StringUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Caffeine caches declared per-name through the {@code cache.config.params}
 * property, so the OTP cache's 5-minute TTL is what enforces OTP expiry - there
 * is no separate expiry column.
 *
 * <p>When {@code enable.cache=false} the manager is returned with no registered
 * caches, which makes {@code CacheManager.getCache(name)} create unbounded
 * dynamic caches. The OTP service depends on TTL for correctness, so caching
 * must be enabled wherever OTP login is used.
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final ObjectMapper objectMapper;

    @Value("${enable.cache:true}")
    private boolean enableCache;

    @Value("${cache.config.params:{}}")
    private String cacheConfigJson;

    @Bean
    @Primary
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofHours(1))
                .recordStats());

        if (!enableCache) {
            return cacheManager;
        }

        try {
            if (StringUtil.isNotBlank(cacheConfigJson)) {
                Map<String, CacheConfigParams> cacheConfigs = objectMapper.readValue(
                        cacheConfigJson, new TypeReference<Map<String, CacheConfigParams>>() {
                        });
                if (!CollectionUtils.isEmpty(cacheConfigs)) {
                    cacheConfigs.forEach((name, config) -> registerCache(cacheManager, name, config));
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse cache configuration '{}'. Using defaults. Error: {}",
                    cacheConfigJson, e.getMessage(), e);
        }
        return cacheManager;
    }

    private void registerCache(CaffeineCacheManager cacheManager, String cacheName, CacheConfigParams config) {
        try {
            Caffeine<Object, Object> caffeine = Caffeine.newBuilder()
                    .maximumSize(config.getMaxSize())
                    .expireAfterWrite(Duration.ofMinutes(config.getTtlMinutes()));
            if (config.isRecordStats()) {
                caffeine.recordStats();
            }
            cacheManager.registerCustomCache(cacheName, caffeine.build());
        } catch (Exception e) {
            log.error("Failed to create cache '{}'. Caused by: {}", cacheName, e.getMessage(), e);
        }
    }
}
