package com.project.promotionservice.partner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.partner.dto.response.PartnerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
public class PartnerCacheService {
    private static final Logger log = LoggerFactory.getLogger(PartnerCacheService.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public PartnerCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void put(PartnerResponse response) {
        if (response == null || response.getPublicId() == null) return;
        afterCommitOrNow(() -> {
            try {
                redis.opsForValue().set(key(response.getPublicId()),
                        objectMapper.writeValueAsString(response), Duration.ofMinutes(15));
            } catch (Exception ex) {
                log.warn("Partner cache refresh failed for {}", response.getPublicId());
            }
        });
    }

    public void evict(String publicId) {
        afterCommitOrNow(() -> {
            try {
                redis.delete(key(publicId));
            } catch (Exception ex) {
                log.warn("Partner cache eviction failed for {}", publicId);
            }
        });
    }

    private String key(String publicId) {
        return "promotion:partner:" + publicId;
    }

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
