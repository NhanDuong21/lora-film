package com.project.promotionservice.integration.job;

import com.project.promotionservice.common.audit.AuditTrailService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.function.IntSupplier;

@Service
public class JobExecutionService {
    private final SchedulerLockService locks;
    private final SchedulerJobExecutionRepository repository;
    private final AuditTrailService audit;

    public JobExecutionService(SchedulerLockService locks,
                               SchedulerJobExecutionRepository repository,
                               AuditTrailService audit) {
        this.locks = locks;
        this.repository = repository;
        this.audit = audit;
    }

    public int run(String jobName, String trigger, String actor, IntSupplier action) {
        String owner = locks.newOwner();
        Instant now = Instant.now();
        if (!locks.tryAcquire(jobName, owner, now)) {
            recordSkipped(jobName, trigger, owner, "Another instance owns the scheduler lease");
            return 0;
        }
        SchedulerJobExecution execution = start(jobName, trigger, owner);
        try {
            int count = action.getAsInt();
            finish(execution.getId(), JobExecutionStatus.SUCCEEDED, count, null);
            audit.recordInNewTransaction("SCHEDULER_JOB", execution.getPublicId(),
                    "JOB_" + jobName.toUpperCase() + "_SUCCEEDED", null, count, actor);
            return count;
        } catch (RuntimeException failure) {
            finish(execution.getId(), JobExecutionStatus.FAILED, 0, limit(failure.getMessage()));
            audit.recordInNewTransaction("SCHEDULER_JOB", execution.getPublicId(),
                    "JOB_" + jobName.toUpperCase() + "_FAILED", null, limit(failure.getMessage()), actor);
            throw failure;
        } finally {
            locks.release(jobName, owner, Instant.now());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    SchedulerJobExecution start(String name, String trigger, String owner) {
        SchedulerJobExecution execution = new SchedulerJobExecution();
        execution.setJobName(name);
        execution.setTriggerType(trigger);
        execution.setInstanceId(owner);
        execution.setStatus(JobExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now());
        return repository.save(execution);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void finish(Long id, JobExecutionStatus status, int count, String error) {
        repository.findById(id).ifPresent(e -> {
            e.setStatus(status);
            e.setProcessedCount(count);
            e.setErrorMessage(error);
            e.setFinishedAt(Instant.now());
            repository.save(e);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void recordSkipped(String jobName, String trigger, String owner, String reason) {
        SchedulerJobExecution execution = new SchedulerJobExecution();
        execution.setJobName(jobName);
        execution.setTriggerType(trigger);
        execution.setInstanceId(owner);
        execution.setStatus(JobExecutionStatus.SKIPPED);
        execution.setStartedAt(Instant.now());
        execution.setFinishedAt(Instant.now());
        execution.setErrorMessage(reason);
        repository.save(execution);
    }

    public java.util.List<SchedulerJobExecution> recent(String jobName) {
        return repository.findByJobNameOrderByStartedAtDesc(jobName, PageRequest.of(0, 20));
    }

    private String limit(String value) {
        if (value == null) return "Unexpected scheduler failure";
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }
}
