package com.project.promotionservice.configuration.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
public class ConfigurationCacheService {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationCacheService.class);
    private static final String PREFIX = "promotion:configuration:";
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ConfigurationCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void put(ConfigurationResponse response) {
        if (response == null || response.getConfigKey() == null) return;
        afterCommitOrNow(() -> {
            try {
                redis.opsForValue().set(key(response.getConfigKey()), response.getConfigValue(), Duration.ofMinutes(10));
            } catch (Exception ex) {
                log.warn("Configuration cache write failed for {}", response.getConfigKey());
            }
        });
    }

    public String get(String configKey) {
        try { return redis.opsForValue().get(key(configKey.trim().toUpperCase())); }
        catch (Exception ex) { return null; }
    }

    public void evict(String configKey) {
        afterCommitOrNow(() -> {
            try { redis.delete(key(configKey)); }
            catch (Exception ex) { log.warn("Configuration cache eviction failed for {}", configKey); }
        });
    }

    public void clear() {
        try {
            var keys = redis.keys(PREFIX + "*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (Exception ex) {
            log.warn("Configuration cache clear failed");
        }
    }

    private String key(String configKey) { return PREFIX + configKey; }

    private void afterCommitOrNow(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
