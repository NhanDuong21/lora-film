package com.project.promotionservice.integration.job;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promotion_scheduler_job_executions")
public class SchedulerJobExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId = UUID.randomUUID().toString();
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;
    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobExecutionStatus status;
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;
    @Column(name = "processed_count", nullable = false)
    private Integer processedCount = 0;
    @Column(name = "error_message", length = 4000)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public JobExecutionStatus getStatus() { return status; }
    public void setStatus(JobExecutionStatus status) { this.status = status; }
    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
