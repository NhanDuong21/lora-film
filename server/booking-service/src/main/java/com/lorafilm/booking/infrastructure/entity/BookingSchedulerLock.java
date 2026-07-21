package com.lorafilm.booking.infrastructure.entity;

import com.lorafilm.booking.common.entity.BaseEntity;
import com.lorafilm.booking.infrastructure.enums.SchedulerLockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_scheduler_locks")
public class BookingSchedulerLock extends BaseEntity {

    @Column(name = "scheduler_name", length = 100, nullable = false, unique = true)
    private String schedulerName;

    @Column(name = "lock_owner", length = 100)
    private String lockOwner;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SchedulerLockStatus status = SchedulerLockStatus.LOCKED;

    public BookingSchedulerLock() {
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public SchedulerLockStatus getStatus() {
        return status;
    }

    public void setStatus(SchedulerLockStatus status) {
        this.status = status;
    }
}
