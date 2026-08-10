package com.project.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notification_scheduled_jobs")
public class NotificationScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;
    @Column(name = "notification_request_id")
    private Long notificationRequestId;
    @Column(name = "job_type", nullable = false, length = 80)
    private String jobType;
    @Column(name = "schedule_expression", length = 120)
    private String scheduleExpression;
    @Column(name = "run_at")
    private Instant runAt;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "lock_owner", length = 100)
    private String lockOwner;
    @Column(name = "lock_until")
    private Instant lockUntil;
    @Column(name = "last_run_at")
    private Instant lastRunAt;
    @Column(name = "next_run_at")
    private Instant nextRunAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationScheduledJob() {
    }

    @PrePersist
    public void beforeInsert() {
        Instant now = Instant.now();
        if (publicId == null) publicId = UUID.randomUUID().toString();
        if (status == null) status = "SCHEDULED";
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public void setPublicId(String value) { this.publicId = value; }
    public Long getNotificationRequestId() { return notificationRequestId; }
    public void setNotificationRequestId(Long value) { this.notificationRequestId = value; }
    public String getJobType() { return jobType; }
    public void setJobType(String value) { this.jobType = value; }
    public String getScheduleExpression() { return scheduleExpression; }
    public void setScheduleExpression(String value) { this.scheduleExpression = value; }
    public Instant getRunAt() { return runAt; }
    public void setRunAt(Instant value) { this.runAt = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getLockOwner() { return lockOwner; }
    public void setLockOwner(String value) { this.lockOwner = value; }
    public Instant getLockUntil() { return lockUntil; }
    public void setLockUntil(Instant value) { this.lockUntil = value; }
    public Instant getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(Instant value) { this.lastRunAt = value; }
    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant value) { this.nextRunAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        return object instanceof NotificationScheduledJob other
                && publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(publicId);
    }

    @Override
    public String toString() {
        return "NotificationScheduledJob{publicId='" + publicId + "', jobType='"
                + jobType + "', status='" + status + "'}";
    }
}
