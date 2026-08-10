package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "business_alerts",
        uniqueConstraints = @UniqueConstraint(name = "uk_alert_insight", columnNames = "insight_id"),
        indexes = @Index(name = "idx_alert_created", columnList = "created_at"))
@Getter
@Setter
@NoArgsConstructor
public class BusinessAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "insight_id", nullable = false)
    private Long insightId;
    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;
    @Column(name = "entity_key", nullable = false, length = 100)
    private String entityKey;
    @Column(nullable = false, length = 20)
    private String severity;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(nullable = false)
    private Boolean acknowledged;
    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;
    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
    @Column(nullable = false)
    private Boolean resolved;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
