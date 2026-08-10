package com.project.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_analytics_snapshots")
public class PaymentAnalyticsSnapshot {
    @Id
    @Column(name = "payment_id")
    private Long paymentId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;
    @Column(name = "movie_id")
    private Long movieId;
    @Column(name = "movie_public_id", columnDefinition = "char(36)")
    private String moviePublicId;
    @Column(name = "movie_title", nullable = false, length = 255)
    private String movieTitle;
    @Column(name = "showtime_public_id", columnDefinition = "char(36)")
    private String showtimePublicId;
    @Column(name = "cinema_public_id", columnDefinition = "char(36)")
    private String cinemaPublicId;
    @Column(name = "auditorium_public_id", columnDefinition = "char(36)")
    private String auditoriumPublicId;
    @Column(name = "showtime_starts_at")
    private Instant showtimeStartsAt;
    @Column(name = "auditorium_capacity")
    private Integer auditoriumCapacity;
    @Column(name = "movie_format", length = 30)
    private String format;
    @Column(name = "ticket_count", nullable = false)
    private Integer ticketCount;
    @Column(name = "ticket_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal ticketAmount = BigDecimal.ZERO;
    @Column(name = "food_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal foodAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "currency", nullable = false, columnDefinition = "char(3)")
    private String currency = "VND";
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public PaymentAnalyticsSnapshot() {
    }
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public String getMoviePublicId() { return moviePublicId; }
    public void setMoviePublicId(String value) { this.moviePublicId = value; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getShowtimePublicId() { return showtimePublicId; }
    public void setShowtimePublicId(String value) { this.showtimePublicId = value; }
    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String value) { this.cinemaPublicId = value; }
    public String getAuditoriumPublicId() { return auditoriumPublicId; }
    public void setAuditoriumPublicId(String value) { this.auditoriumPublicId = value; }
    public Instant getShowtimeStartsAt() { return showtimeStartsAt; }
    public void setShowtimeStartsAt(Instant value) { this.showtimeStartsAt = value; }
    public Integer getAuditoriumCapacity() { return auditoriumCapacity; }
    public void setAuditoriumCapacity(Integer value) { this.auditoriumCapacity = value; }
    public String getFormat() { return format; }
    public void setFormat(String value) { this.format = value; }
    public Integer getTicketCount() { return ticketCount; }
    public void setTicketCount(Integer ticketCount) { this.ticketCount = ticketCount; }
    public BigDecimal getTicketAmount() { return ticketAmount; }
    public void setTicketAmount(BigDecimal value) { this.ticketAmount = value; }
    public BigDecimal getFoodAmount() { return foodAmount; }
    public void setFoodAmount(BigDecimal value) { this.foodAmount = value; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal value) { this.discountAmount = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { this.totalAmount = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
