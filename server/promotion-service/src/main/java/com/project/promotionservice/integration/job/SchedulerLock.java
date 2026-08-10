package com.project.promotionservice.integration.job;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "promotion_scheduler_locks")
public class SchedulerLock {
    @Id
    @Column(name = "job_name", length = 100)
    private String jobName;
    @Column(name = "owner", nullable = false, length = 100)
    private String owner;
    @Column(name = "locked_until", nullable = false)
    private Instant lockedUntil;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
