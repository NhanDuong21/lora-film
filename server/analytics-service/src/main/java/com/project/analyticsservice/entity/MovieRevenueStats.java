package com.project.analyticsservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;


public class MovieRevenueStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movie_id", nullable = false, unique = true)
    private Long movieId;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "total_tickets_sold", nullable = false)
    private Integer totalTicketsSold;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}