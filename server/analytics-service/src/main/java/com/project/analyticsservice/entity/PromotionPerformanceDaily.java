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
@Table(name = "promotion_performance_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_promotion_date", columnNames = {"promotion_key", "stat_date"}),
        indexes = @Index(name = "idx_promotion_kpi_date", columnList = "stat_date"))
@Getter
@Setter
@NoArgsConstructor
public class PromotionPerformanceDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "promotion_key", nullable = false, length = 100)
    private String promotionKey;
    @Column(name = "promotion_name")
    private String promotionName;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "usage_count", nullable = false)
    private Long usageCount;
    @Column(name = "discount_cost", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountCost;
    @Column(name = "generated_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal generatedRevenue;
    @Column(name = "roi", nullable = false, precision = 19, scale = 6)
    private BigDecimal roi;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
