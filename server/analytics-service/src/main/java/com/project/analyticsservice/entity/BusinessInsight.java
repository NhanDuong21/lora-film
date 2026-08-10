package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "business_insights", indexes = {
        @Index(name = "idx_insight_date", columnList = "stat_date"),
        @Index(name = "idx_insight_severity", columnList = "severity")
})
@Getter
@Setter
@NoArgsConstructor
public class BusinessInsight {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 180)
    private String fingerprint;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(nullable = false, length = 100)
    private String category;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "json")
    private String evidenceJson;
    @Column(name = "baseline_start_date")
    private LocalDate baselineStartDate;
    @Column(name = "baseline_end_date")
    private LocalDate baselineEndDate;
    @Column(name = "expected_value", precision = 19, scale = 6)
    private BigDecimal expectedValue;
    @Column(name = "actual_value", precision = 19, scale = 6)
    private BigDecimal actualValue;
    @Column(name = "deviation_rate", precision = 12, scale = 6)
    private BigDecimal deviationRate;
    @Column(name = "analysis_version", length = 64)
    private String analysisVersion;
    @Column(name = "confidence_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal confidenceScore;
    @Column(nullable = false)
    private Boolean resolved;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at")
    private Instant updatedAt;
}
