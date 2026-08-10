package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "forecast_results",
        uniqueConstraints = @UniqueConstraint(name = "uk_forecast_result",
                columnNames = {"entity_type", "entity_key", "forecast_date", "forecast_type"}),
        indexes = @Index(name = "idx_forecast_date", columnList = "forecast_date"))
@Getter
@Setter
@NoArgsConstructor
public class ForecastResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;
    @Column(name = "forecast_type", nullable = false, length = 30)
    private String forecastType;
    @Column(name = "as_of_date")
    private LocalDate asOfDate;
    @Column(name = "predicted_value", nullable = false, precision = 19, scale = 6)
    private BigDecimal predictedValue;
    @Column(name = "prediction_lower_bound", precision = 19, scale = 6)
    private BigDecimal predictionLowerBound;
    @Column(name = "prediction_upper_bound", precision = 19, scale = 6)
    private BigDecimal predictionUpperBound;
    @Column(name = "confidence_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal confidenceScore;
    @Column(nullable = false, length = 100)
    private String algorithm;
    @Column(name = "model_version", length = 64)
    private String modelVersion;
    @Column(name = "training_start_date", nullable = false)
    private LocalDate trainingStartDate;
    @Column(name = "training_end_date", nullable = false)
    private LocalDate trainingEndDate;
    @UpdateTimestamp @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
    @Version
    private Long version;
}
