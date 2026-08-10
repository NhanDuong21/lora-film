package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "recommendations",
        uniqueConstraints = @UniqueConstraint(name = "uk_recommendation_insight_action",
                columnNames = {"insight_id", "action_type"}))
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "insight_id", nullable = false)
    private Long insightId;
    @Column(name = "target_service", nullable = false, length = 100)
    private String targetService;
    @Column(name = "action_type", nullable = false, length = 100)
    private String actionType;
    @Column(nullable = false, length = 20)
    private String priority;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(name = "expected_impact", nullable = false, columnDefinition = "TEXT")
    private String expectedImpact;
    @Column(name = "estimated_impact_value", precision = 19, scale = 6)
    private BigDecimal estimatedImpactValue;
    @Column(name = "impact_unit", length = 30)
    private String impactUnit;
    @Column(name = "confidence_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal confidenceScore;
    @Column(nullable = false, length = 30)
    private String status;
    @Column(name = "accepted_by", length = 100)
    private String acceptedBy;
    @Column(name = "accepted_at")
    private Instant acceptedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
