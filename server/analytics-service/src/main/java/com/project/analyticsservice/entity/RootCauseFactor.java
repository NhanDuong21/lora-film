package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "root_cause_factors",
        uniqueConstraints = @UniqueConstraint(name = "uk_root_cause_insight_rank",
                columnNames = {"insight_id", "rank_order"}))
@Getter
@Setter
@NoArgsConstructor
public class RootCauseFactor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "insight_id", nullable = false)
    private Long insightId;
    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;
    @Column(name = "cause_type", nullable = false, length = 100)
    private String causeType;
    @Column(name = "dimension_type", length = 50)
    private String dimensionType;
    @Column(name = "dimension_key", length = 100)
    private String dimensionKey;
    @Column(name = "contribution_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal contributionScore;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_json", columnDefinition = "json")
    private String evidenceJson;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
