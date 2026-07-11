package com.project.scoreservice.client;
 
import java.time.LocalDateTime;
 
public class BookingContext {
    private Long bookingId;
    private Long userId;
    private String status;
    private LocalDateTime expiresAt;
    private boolean redeemAllowed;
 
    public BookingContext() {
    }
 
    public BookingContext(Long bookingId, Long userId, String status, LocalDateTime expiresAt, boolean redeemAllowed) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.redeemAllowed = redeemAllowed;
    }
 
    public Long getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
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
 
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
 
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
 
    public boolean isRedeemAllowed() {
        return redeemAllowed;
    }
 
    public void setRedeemAllowed(boolean redeemAllowed) {
        this.redeemAllowed = redeemAllowed;
    }
}
