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
    public void setBookingPublicId(String bookingPublicId) { this.bookingPublicId = bookingPublicId; }

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
    public void setAmountLockedAt(Instant amountLockedAt) { this.amountLockedAt = amountLockedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public AnalyticsSnapshotData getAnalyticsSnapshot() { return analyticsSnapshot; }
    public void setAnalyticsSnapshot(AnalyticsSnapshotData analyticsSnapshot) { this.analyticsSnapshot = analyticsSnapshot; }

    public static class AnalyticsSnapshotData {
        private Long movieId;
        private String movieTitle;
        private Integer ticketCount;

        public AnalyticsSnapshotData() {
        }

        public Long getMovieId() { return movieId; }
        public void setMovieId(Long movieId) { this.movieId = movieId; }

        public String getMovieTitle() { return movieTitle; }
        public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

        public Integer getTicketCount() { return ticketCount; }
        public void setTicketCount(Integer ticketCount) { this.ticketCount = ticketCount; }
    }
}
