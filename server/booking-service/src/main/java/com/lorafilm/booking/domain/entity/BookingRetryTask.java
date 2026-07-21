package com.lorafilm.booking.domain.entity;

import com.lorafilm.booking.domain.enums.RetryReferenceType;
import com.lorafilm.booking.domain.enums.RetryTaskStatus;
import com.lorafilm.booking.domain.enums.RetryTaskType;
import com.lorafilm.booking.domain.enums.TaskPriority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "booking_retry_tasks")
public class BookingRetryTask extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false)
    private RetryTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private RetryReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "payload", columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private TaskPriority priority = TaskPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RetryTaskStatus status = RetryTaskStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    private Integer maxRetryCount = 10;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public BookingRetryTask() {
    }

    public RetryTaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(RetryTaskType taskType) {
        this.taskType = taskType;
    }

    public RetryReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RetryReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public RetryTaskStatus getStatus() {
        return status;
    }

    public void setStatus(RetryTaskStatus status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetryCount() {
        return maxRetryCount;
    }

    public void setMaxRetryCount(Integer maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public Instant getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Instant nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public Instant getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(Instant lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
