package com.lorafilm.booking.infrastructure.lock;

import com.lorafilm.booking.infrastructure.entity.BookingSchedulerLock;
import com.lorafilm.booking.infrastructure.enums.SchedulerLockStatus;
import com.lorafilm.booking.infrastructure.repository.BookingSchedulerLockRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Aspect
@Component
public class SchedulerLockAspect {

    private static final Logger log = LoggerFactory.getLogger(SchedulerLockAspect.class);
    private final BookingSchedulerLockRepository lockRepository;
    private final String ownerId = "node-" + UUID.randomUUID().toString().substring(0, 8);

    public SchedulerLockAspect(BookingSchedulerLockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Around("@annotation(schedulerLock)")
    public Object manageLock(ProceedingJoinPoint joinPoint, SchedulerLock schedulerLock) throws Throwable {
        String lockName = schedulerLock.name();
        long lockSeconds = schedulerLock.lockAtMostForSeconds();
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(lockSeconds);

        boolean acquired = false;
        try {
            int updated = lockRepository.tryAcquireLock(lockName, ownerId, now, expiresAt);
            if (updated > 0) {
                acquired = true;
            } else {
                if (lockRepository.findBySchedulerName(lockName).isEmpty()) {
                    try {
                        BookingSchedulerLock lock = new BookingSchedulerLock();
                        lock.setSchedulerName(lockName);
                        lock.setLockOwner(ownerId);
                        lock.setLockedAt(now);
                        lock.setExpiresAt(expiresAt);
                        lock.setStatus(SchedulerLockStatus.LOCKED);
                        lockRepository.saveAndFlush(lock);
                        acquired = true;
                    } catch (DataIntegrityViolationException ex) {
                        int updatedSecondTry = lockRepository.tryAcquireLock(lockName, ownerId, now, expiresAt);
                        if (updatedSecondTry > 0) {
                            acquired = true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error checking/acquiring scheduler lock: {}", lockName, ex);
        }

        if (!acquired) {
            log.debug("Scheduler lock [{}] is already held by another node. Skipping execution.", lockName);
            return null;
        }

        log.info("Successfully acquired scheduler lock [{}]", lockName);
        try {
            return joinPoint.proceed();
        } finally {
            try {
                lockRepository.releaseLock(lockName, ownerId);
                log.info("Released scheduler lock [{}]", lockName);
            } catch (Exception ex) {
                log.error("Failed to release scheduler lock: {}", lockName, ex);
            }
        }
    }
}
