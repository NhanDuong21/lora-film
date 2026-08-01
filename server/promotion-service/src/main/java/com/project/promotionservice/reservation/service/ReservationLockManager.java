package com.project.promotionservice.reservation.service;

import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.lock.RedisLockService;
import com.project.promotionservice.reservation.exception.ReservationErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Component
public class ReservationLockManager {

    private final RedisLockService redisLockService;
    private final boolean enabled;
    private final Duration lockTtl;
    private final Duration lockWait;

    public ReservationLockManager(
            RedisLockService redisLockService,
            @Value("${promotion.reservation.distributed-lock-enabled:true}") boolean enabled,
            @Value("${promotion.reservation.lock-ttl-seconds:30}") long lockTtlSeconds,
            @Value("${promotion.reservation.lock-wait-ms:1000}") long lockWaitMillis) {
        this.redisLockService = redisLockService;
        this.enabled = enabled;
        this.lockTtl = Duration.ofSeconds(Math.max(5, lockTtlSeconds));
        this.lockWait = Duration.ofMillis(Math.max(0, lockWaitMillis));
    }

    public void lockPromotion(String promotionPublicId) {
        acquire("promotion:template:" + promotionPublicId.toLowerCase(Locale.ROOT));
    }

    public void lockReservation(String reservationPublicId) {
        acquire("promotion:reservation:" + reservationPublicId.toLowerCase(Locale.ROOT));
    }

    public void lockCheckout(String orderPublicId, String bookingPublicId) {
        String scope = orderPublicId != null && !orderPublicId.isBlank()
                ? "order:" + orderPublicId
                : "booking:" + bookingPublicId;
        acquire("promotion:checkout:" + scope.toLowerCase(Locale.ROOT));
    }

    private void acquire(String key) {
        if (!enabled) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Reservation distributed lock requires an active transaction");
        }

        String owner = UUID.randomUUID().toString();
        long deadlineNanos = System.nanoTime() + lockWait.toNanos();
        RedisLockService.LockAttempt attempt;
        do {
            attempt = redisLockService.tryAcquire(key, owner, lockTtl);
            if (attempt == RedisLockService.LockAttempt.ACQUIRED) {
                registerRelease(key, owner);
                return;
            }
            if (attempt == RedisLockService.LockAttempt.UNAVAILABLE) {
                return;
            }
            if (System.nanoTime() >= deadlineNanos) {
                break;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            }
        } while (true);

        throw new BusinessException(
                ReservationErrorCode.RESERVATION_CONFLICT,
                "Reservation resource is being processed by another request",
                HttpStatus.CONFLICT);
    }

    private void registerRelease(String key, String owner) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisLockService.release(key, owner);
            throw new IllegalStateException("Reservation transaction synchronization is no longer active");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                redisLockService.release(key, owner);
            }
        });
    }
}
