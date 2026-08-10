package com.project.analyticsservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fact_booking_metrics", indexes = {
        @Index(name = "idx_fact_booking_date", columnList = "business_date"),
        @Index(name = "idx_fact_booking_key", columnList = "booking_public_id"),
        @Index(name = "idx_fact_movie_date", columnList = "movie_key,business_date"),
        @Index(name = "idx_fact_cinema_date", columnList = "cinema_public_id,business_date")
})
@Getter
@Setter
@NoArgsConstructor
public class FactBookingMetric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 150)
    private String eventId;
    @Column(name = "payment_public_id", nullable = false, length = 64)
    private String paymentPublicId;
    @Column(name = "booking_public_id", nullable = false, length = 64)
    private String bookingPublicId;
    @Column(name = "user_public_id", length = 64)
    private String userPublicId;
    @Column(name = "movie_id")
    private Long movieId;
    @Column(name = "movie_key", nullable = false, length = 100)
    private String movieKey;
    @Column(name = "movie_public_id", length = 64)
    private String moviePublicId;
    @Column(name = "movie_title", nullable = false)
    private String movieTitle;
    @Column(name = "cinema_public_id", length = 100)
    private String cinemaPublicId;
    @Column(name = "cinema_name")
    private String cinemaName;
    @Column(name = "auditorium_public_id", length = 100)
    private String auditoriumPublicId;
    @Column(name = "showtime_public_id", length = 100)
    private String showtimePublicId;
    @Column(name = "showtime_starts_at")
    private Instant showtimeStartsAt;
    @Column(name = "movie_format", length = 30)
    private String format;
    @Column(name = "promotion_public_id", length = 100)
    private String promotionPublicId;
    @Column(name = "promotion_name")
    private String promotionName;
    @Column(name = "membership_tier", length = 50)
    private String membershipTier;
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "net_revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal netRevenue;
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;
    @Column(name = "available_seats")
    private Integer availableSeats;
    @Column(name = "booking_status", nullable = false, length = 30)
    private String bookingStatus;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
