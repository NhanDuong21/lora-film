package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.AnalyticsCommands;
import com.project.analyticsservice.dto.AnalyticsResponses;
import com.project.analyticsservice.entity.AnalyticsJobRun;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.AnalyticsJobRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsJobDomainService {
    private final AnalyticsJobRunRepository jobRepository;
    private final KpiPipelineDomainService pipeline;

    public AnalyticsJobDomainService(
            AnalyticsJobRunRepository jobRepository,
            KpiPipelineDomainService pipeline) {
        this.jobRepository = jobRepository;
        this.pipeline = pipeline;
    }

    public AnalyticsResponses.Job submit(
            AnalyticsCommands.RebuildJob request, String requestedBy) {
        AnalyticsJobRun existing = jobRepository.findByRequestId(request.requestId()).orElse(null);
        if (existing != null) {
            return response(existing);
        }
        if (request.startDate().isAfter(request.endDate())) {
            throw invalid("startDate must not be after endDate");
        }
        long totalDays = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (totalDays > 367) {
            throw invalid("A rebuild job cannot exceed 367 days");
        }
        AnalyticsJobRun job = new AnalyticsJobRun();
        job.setRequestId(request.requestId());
        job.setJobType("REBUILD");
        job.setMode(request.mode() == null || request.mode().isBlank()
                ? "UPSERT" : request.mode().trim().toUpperCase());
        job.setStartDate(request.startDate());
        job.setEndDate(request.endDate());
        job.setStatus("QUEUED");
        job.setRequestedBy(requestedBy == null || requestedBy.isBlank()
                ? "unknown" : requestedBy);
        job.setRequestedAt(Instant.now());
        job.setProcessedDays(0);
        job.setTotalDays(Math.toIntExact(totalDays));
        return response(jobRepository.save(job));
    }

    public List<AnalyticsResponses.Job> recent() {
        return jobRepository.findTop20ByOrderByRequestedAtDesc()
                .stream().map(this::response).toList();
    }

    public void run(long jobId) {
        AnalyticsJobRun job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus("RUNNING");
        job.setStartedAt(Instant.now());
        jobRepository.save(job);
        try {
            LocalDate current = job.getStartDate();
            while (!current.isAfter(job.getEndDate())) {
                pipeline.calculate(current);
                job.setProcessedDays(job.getProcessedDays() + 1);
                jobRepository.save(job);
                current = current.plusDays(1);
            }
            job.setStatus("SUCCESS");
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        } catch (RuntimeException exception) {
            job.setStatus("FAILED");
            job.setErrorMessage(limit(exception.getMessage()));
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private AnalyticsResponses.Job response(AnalyticsJobRun job) {
        return new AnalyticsResponses.Job(
                job.getId(), job.getRequestId(), job.getJobType(), job.getMode(),
                job.getStartDate(), job.getEndDate(), job.getStatus(),
                job.getRequestedBy(), job.getRequestedAt(), job.getStartedAt(),
                job.getCompletedAt(), job.getProcessedDays(), job.getTotalDays(),
                job.getErrorMessage());
    }

    private BusinessException invalid(String message) {
        return new BusinessException(
                message, "ANALYTICS_INVALID_REBUILD_JOB", HttpStatus.BAD_REQUEST);
    }

    private String limit(String message) {
        String value = message == null ? "Unknown analytics rebuild failure" : message;
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
