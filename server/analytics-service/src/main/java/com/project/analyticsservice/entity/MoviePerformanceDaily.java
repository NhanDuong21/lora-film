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
@Table(name = "movie_performance_daily",
        uniqueConstraints = @UniqueConstraint(name = "uk_movie_date", columnNames = {"movie_key", "stat_date"}),
        indexes = @Index(name = "idx_movie_kpi_date", columnList = "stat_date"))
@Getter
@Setter
@NoArgsConstructor
public class MoviePerformanceDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "movie_key", nullable = false, length = 100)
    private String movieKey;
    @Column(name = "movie_id")
    private Long movieId;
    @Column(name = "movie_title", nullable = false)
    private String movieTitle;
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;
    @Column(name = "gross_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossRevenue;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    @Column(name = "net_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal netRevenue;
    @Column(name = "booking_count", nullable = false)
    private Long bookingCount;
    @Column(name = "ticket_count", nullable = false)
    private Long ticketCount;
    @Column(name = "occupancy_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal occupancyRate;
    @Column(name = "refund_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal refundRate;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private Long version;
}
