package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "analytics_data_quality_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_data_quality_source_date",
                columnNames = {"stat_date", "source_service", "event_type"}))
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsDataQualityDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    @Column(name = "received_count", nullable = false)
    private Long receivedCount;
    @Column(name = "accepted_count", nullable = false)
    private Long acceptedCount;
    @Column(name = "duplicate_count", nullable = false)
    private Long duplicateCount;
    @Column(name = "rejected_count", nullable = false)
    private Long rejectedCount;
    @Column(name = "dlq_count", nullable = false)
    private Long dlqCount;
    @Column(name = "late_event_count", nullable = false)
    private Long lateEventCount;
    @Column(name = "average_lag_seconds", precision = 19, scale = 6)
    private BigDecimal averageLagSeconds;
    @Column(name = "maximum_lag_seconds")
    private Long maximumLagSeconds;
    @Column(name = "completeness_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal completenessScore;
    @Column(name = "freshness_status", nullable = false, length = 30)
    private String freshnessStatus;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
