package com.project.promotionservice.integration.job;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SchedulerLockService {
    private final SchedulerLockRepository repository;
    private final Duration lease;

    public SchedulerLockService(SchedulerLockRepository repository,
                                @Value("${promotion.scheduler.lock-seconds:300}") long leaseSeconds) {
        this.repository = repository;
        this.lease = Duration.ofSeconds(Math.max(30, leaseSeconds));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquire(String jobName, String owner, Instant now) {
        try {
            SchedulerLock lock = repository.findForUpdate(jobName).orElse(null);
            if (lock == null) {
                lock = new SchedulerLock();
                lock.setJobName(jobName);
                lock.setOwner(owner);
                lock.setLockedUntil(now.plus(lease));
                lock.setUpdatedAt(now);
                repository.saveAndFlush(lock);
                return true;
            }
            if (lock.getLockedUntil().isAfter(now) && !owner.equals(lock.getOwner())) {
                return false;
            }
            lock.setOwner(owner);
            lock.setLockedUntil(now.plus(lease));
            lock.setUpdatedAt(now);
            repository.save(lock);
            return true;
        } catch (DataIntegrityViolationException race) {
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String jobName, String owner, Instant now) {
        repository.findForUpdate(jobName).ifPresent(lock -> {
            if (owner.equals(lock.getOwner())) {
                lock.setLockedUntil(now);
                lock.setUpdatedAt(now);
                repository.save(lock);
            }
        });
    }

    public String newOwner() {
        return "scheduler-" + UUID.randomUUID();
    }
}
