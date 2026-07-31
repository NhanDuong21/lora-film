package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_job_runs")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsJobRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;
    @Column(name = "job_type", nullable = false, length = 30)
    private String jobType;
    @Column(nullable = false, length = 30)
    private String mode;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "processed_days", nullable = false)
    private Integer processedDays;
    @Column(name = "total_days", nullable = false)
    private Integer totalDays;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
