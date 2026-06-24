package com.project.analyticsservice.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class DailyRevenueStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stat_date", nullable = false, unique = true)
    private LocalDate statDate;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "total_bookings_count", nullable = false)
    private Integer totalBookingsCount;

    @Column(name = "cancelled_bookings_count", nullable = false)
    private Integer cancelledBookingsCount;

    @Column(name = "total_tickets_sold", nullable = false)
    private Integer totalTicketsSold;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}