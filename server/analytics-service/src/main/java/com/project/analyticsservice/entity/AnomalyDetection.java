package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "anomaly_detections")
@Getter
@Setter
@NoArgsConstructor
public class AnomalyDetection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 180)
    private String fingerprint;
    @Column(name = "insight_id")
    private Long insightId;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;
    @Column(name = "actual_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal actualValue;
    @Column(name = "expected_value", precision = 19, scale = 6)
    private BigDecimal expectedValue;
    @Column(name = "deviation_rate", precision = 12, scale = 6)
    private BigDecimal deviationRate;
    @Column(name = "anomaly_score", nullable = false, precision = 19, scale = 6)
    private BigDecimal anomalyScore;
    @Column(name = "detection_method", nullable = false, length = 100)
    private String detectionMethod;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(nullable = false, length = 30)
    private String status;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "json")
    private String evidenceJson;
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
