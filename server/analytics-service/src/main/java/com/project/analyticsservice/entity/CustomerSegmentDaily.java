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
@Table(name = "customer_segment_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_segment_date", columnNames = {"membership_tier", "stat_date"}),
        indexes = @Index(name = "idx_segment_kpi_date", columnList = "stat_date"))
@Getter
@Setter
@NoArgsConstructor
public class CustomerSegmentDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "membership_tier", nullable = false, length = 50)
    private String membershipTier;
    @Column(name = "active_users", nullable = false)
    private Long activeUsers;
    @Column(name = "new_users", nullable = false)
    private Long newUsers;
    @Column(name = "returning_users", nullable = false)
    private Long returningUsers;
    @Column(name = "total_spending", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSpending;
    @Column(name = "average_spending", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageSpending;
    @Column(name = "customer_lifetime_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal customerLifetimeValue;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
