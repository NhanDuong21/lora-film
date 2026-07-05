package com.project.bookingservice.dto.payment;

import com.project.bookingservice.enumtype.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentContextResponse {

    private Long bookingId;
    private Long accountId;
    private BookingStatus bookingStatus;
    private boolean payable;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime expiresAt;
    private AnalyticsSnapshot analyticsSnapshot;

    public PaymentContextResponse() {
    }

    public static class AnalyticsSnapshot {
        private Long movieId;
        private String movieTitle;
        private int ticketCount;

        public AnalyticsSnapshot() {
        }

        public AnalyticsSnapshot(Long movieId, String movieTitle, int ticketCount) {
            this.movieId = movieId;
            this.movieTitle = movieTitle;
            this.ticketCount = ticketCount;
        }

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public String getMovieTitle() {
            return movieTitle;
        }

        public void setMovieTitle(String movieTitle) {
            this.movieTitle = movieTitle;
        }

        public int getTicketCount() {
            return ticketCount;
        }

        public void setTicketCount(int ticketCount) {
            this.ticketCount = ticketCount;
        }
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public boolean isPayable() {
        return payable;
    }

    public void setPayable(boolean payable) {
        this.payable = payable;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public AnalyticsSnapshot getAnalyticsSnapshot() {
        return analyticsSnapshot;
    }

    public void setAnalyticsSnapshot(AnalyticsSnapshot analyticsSnapshot) {
        this.analyticsSnapshot = analyticsSnapshot;
    }
}
