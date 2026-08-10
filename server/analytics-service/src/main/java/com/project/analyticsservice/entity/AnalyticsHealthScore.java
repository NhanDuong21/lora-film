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
@Table(name = "analytics_health_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_health_score_entity_date",
                columnNames = {"entity_type", "entity_key", "stat_date"}))
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsHealthScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "overall_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal overallScore;
    @Column(name = "revenue_score", precision = 12, scale = 6)
    private BigDecimal revenueScore;
    @Column(name = "demand_score", precision = 12, scale = 6)
    private BigDecimal demandScore;
    @Column(name = "occupancy_score", precision = 12, scale = 6)
    private BigDecimal occupancyScore;
    @Column(name = "customer_score", precision = 12, scale = 6)
    private BigDecimal customerScore;
    @Column(name = "operational_score", precision = 12, scale = 6)
    private BigDecimal operationalScore;
    @Column(name = "data_quality_score", precision = 12, scale = 6)
    private BigDecimal dataQualityScore;
    @Column(name = "health_status", nullable = false, length = 30)
    private String healthStatus;
    @Column(name = "confidence_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal confidenceScore;
    @Column(name = "algorithm_version", nullable = false, length = 64)
    private String algorithmVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "drivers_json", columnDefinition = "json")
    private String driversJson;
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
    @Version
    private Long version;
}
