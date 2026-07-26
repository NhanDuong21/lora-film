package com.project.promotionservice.common.lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisLockService {

    private static final Logger log = LoggerFactory.getLogger(RedisLockService.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquireLock(String key, String owner, long ttlSeconds) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, owner, Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(success);
        } catch (Exception ex) {
            log.error("Failed to acquire Redis lock for key: {}", key, ex);
            return false;
        }
    }

    public void releaseLock(String key, String owner) {
        try {
            String currentOwner = redisTemplate.opsForValue().get(key);
            if (owner.equals(currentOwner)) {
                redisTemplate.delete(key);
            }
        } catch (Exception ex) {
            log.error("Failed to release Redis lock for key: {}", key, ex);
        }
    }
}
