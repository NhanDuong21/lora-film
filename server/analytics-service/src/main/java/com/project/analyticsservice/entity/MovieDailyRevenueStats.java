package com.project.analyticsservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class MovieDailyRevenueStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "tickets_sold", nullable = false)
    private Integer ticketsSold;

    @Column(name = "revenue", nullable = false)
    private BigDecimal revenue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
