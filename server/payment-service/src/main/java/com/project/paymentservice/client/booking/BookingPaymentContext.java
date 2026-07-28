package com.project.paymentservice.client.booking;

import java.math.BigDecimal;
import java.time.Instant;

public class BookingPaymentContext {
    private Long bookingId;
    private String bookingPublicId;
    private Long accountId;
    private String bookingStatus;
    private Boolean payable;
    private BigDecimal amount;
    private String currency;
    private Instant amountLockedAt;
    private Instant expiresAt;
    private AnalyticsSnapshotData analyticsSnapshot;

    public BookingPaymentContext() {
    }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public String getBookingPublicId() { return bookingPublicId; }
    public void setBookingPublicId(String value) { this.bookingPublicId = value; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getBookingStatus() { return bookingStatus; }
    public void setBookingStatus(String bookingStatus) { this.bookingStatus = bookingStatus; }
    public Boolean getPayable() { return payable; }
    public void setPayable(Boolean payable) { this.payable = payable; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getAmountLockedAt() { return amountLockedAt; }
    public void setAmountLockedAt(Instant value) { this.amountLockedAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public AnalyticsSnapshotData getAnalyticsSnapshot() { return analyticsSnapshot; }
    public void setAnalyticsSnapshot(AnalyticsSnapshotData value) { this.analyticsSnapshot = value; }

    public static class AnalyticsSnapshotData {
        private Long movieId;
        private String moviePublicId;
        private String movieTitle;
        private String showtimePublicId;
        private String cinemaPublicId;
        private Integer ticketCount;
        private BigDecimal ticketAmount;
        private BigDecimal foodAmount;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        private String currency;

        public AnalyticsSnapshotData() {
        }
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
        public Integer getTicketCount() { return ticketCount; }
        public void setTicketCount(Integer value) { this.ticketCount = value; }
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
    }
}
