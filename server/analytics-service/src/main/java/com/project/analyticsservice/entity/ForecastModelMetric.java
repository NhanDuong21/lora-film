package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "forecast_model_metrics",
        uniqueConstraints = @UniqueConstraint(name = "uk_forecast_model_evaluation",
                columnNames = {"entity_type", "entity_key", "forecast_type", "model_version", "evaluation_date"}))
@Getter
@Setter
@NoArgsConstructor
public class ForecastModelMetric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(name = "forecast_type", nullable = false, length = 30)
    private String forecastType;
    @Column(nullable = false, length = 100)
    private String algorithm;
    @Column(name = "model_version", nullable = false, length = 64)
    private String modelVersion;
    @Column(name = "evaluation_date", nullable = false)
    private LocalDate evaluationDate;
    @Column(name = "test_start_date", nullable = false)
    private LocalDate testStartDate;
    @Column(name = "test_end_date", nullable = false)
    private LocalDate testEndDate;
    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;
    @Column(precision = 19, scale = 6)
    private BigDecimal mae;
    @Column(precision = 19, scale = 6)
    private BigDecimal rmse;
    @Column(precision = 12, scale = 6)
    private BigDecimal mape;
    @Column(precision = 19, scale = 6)
    private BigDecimal bias;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
