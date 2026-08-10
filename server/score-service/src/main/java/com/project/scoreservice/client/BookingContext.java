package com.project.scoreservice.client;
 
import java.math.BigDecimal;
import java.time.Instant;
 
public class BookingContext {
    private Long bookingId;
    private String bookingPublicId;
    private Long userId;
    private String status;
    private Instant expiresAt;
    private boolean redeemAllowed;
    private BigDecimal amount;
 
    public BookingContext() {
    }
 
    public BookingContext(
            Long bookingId,
            String bookingPublicId,
            Long userId,
            String status,
            Instant expiresAt,
            boolean redeemAllowed,
            BigDecimal amount) {
        this.bookingId = bookingId;
        this.bookingPublicId = bookingPublicId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.redeemAllowed = redeemAllowed;
        this.amount = amount;
    }
 
    public Long getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getBookingPublicId() {
        return bookingPublicId;
    }

    public void setBookingPublicId(String bookingPublicId) {
        this.bookingPublicId = bookingPublicId;
    }
 
    public Long getUserId() {
        return userId;
    }
 
    public void setUserId(Long userId) {
        this.userId = userId;
    }
 
    public String getStatus() {
        return status;
    }
 
    public void setStatus(String status) {
        this.status = status;
    }
 
    public Instant getExpiresAt() {
        return expiresAt;
    }
 
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
 
    public boolean isRedeemAllowed() {
        return redeemAllowed;
    }
 
    public void setRedeemAllowed(boolean redeemAllowed) {
        this.redeemAllowed = redeemAllowed;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
