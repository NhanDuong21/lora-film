package com.project.userservice.service;

import com.project.userservice.dto.ReservationResult;
import com.project.userservice.service.impl.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock StringRedisTemplate redisTemplate;

    @Test
    void atomicScriptSuccessIsIdempotentForTheRequestOwner() {
        when(redisTemplate.execute(
                isA(RedisScript.class),
                eq(List.of("reserved_phone:0901234567", "reserved_cccd:092205006789")),
                eq("request-1"),
                eq("900000"))).thenReturn(0L);

        ReservationResult result = new ReservationService(redisTemplate).reserve(
                "0901234567", "092205006789", "request-1", Duration.ofMinutes(15));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void phoneConflictReturnsItsRemainingTtl() {
        when(redisTemplate.execute(
                isA(RedisScript.class),
                eq(List.of("reserved_phone:0901234567", "reserved_cccd:092205006789")),
                eq("request-2"),
                eq("900000"))).thenReturn(1L);
        when(redisTemplate.getExpire("reserved_phone:0901234567", TimeUnit.SECONDS))
                .thenReturn(42L);

        ReservationResult result = new ReservationService(redisTemplate).reserve(
                "0901234567", "092205006789", "request-2", Duration.ofMinutes(15));

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("PHONE_NUMBER_RESERVED");
        assertThat(result.getRetryAfterSeconds()).isEqualTo(42L);
    }
}
