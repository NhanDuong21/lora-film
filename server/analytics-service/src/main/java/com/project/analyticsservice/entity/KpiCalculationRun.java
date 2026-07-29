package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "kpi_calculation_runs", indexes =
        @Index(name = "idx_kpi_run_date", columnList = "stat_date,started_at"))
@Getter
@Setter
@NoArgsConstructor
public class KpiCalculationRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "run_id", nullable = false, unique = true, length = 64)
    private String runId;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "completed_stage", length = 60)
    private String completedStage;
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
}
