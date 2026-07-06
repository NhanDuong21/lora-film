package com.project.scoreservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ScoreRefundRequest {

    @NotNull(message = "User ID must be specified")
    @Positive(message = "User ID must be positive")
    private Long userId;

    @NotNull(message = "Booking ID must be specified")
    @Positive(message = "Booking ID must be positive")
    private Long bookingId;

    @NotNull(message = "Points must be specified")
    @Min(value = 1, message = "Points must be greater than zero")
    private Integer points;

    @NotBlank(message = "Original redeem event ID must be specified")
    @Size(max = 150, message = "Original redeem event ID cannot exceed 150 characters")
    private String originalRedeemEventId;

    @NotBlank(message = "Event ID must be specified")
    @Size(max = 150, message = "Event ID cannot exceed 150 characters")
    private String eventId;

    @NotBlank(message = "Idempotency key must be specified")
    @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
    private String idempotencyKey;

    @NotBlank(message = "Reason must be specified")
    @Size(max = 255, message = "Reason cannot exceed 255 characters")
    private String reason;

    public ScoreRefundRequest() {
    }

    public ScoreRefundRequest(Long userId, Long bookingId, Integer points, String originalRedeemEventId, String eventId, String idempotencyKey, String reason) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.points = points;
        this.originalRedeemEventId = originalRedeemEventId;
        this.eventId = eventId;
        this.idempotencyKey = idempotencyKey;
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getOriginalRedeemEventId() {
        return originalRedeemEventId;
    }

    public void setOriginalRedeemEventId(String originalRedeemEventId) {
        this.originalRedeemEventId = originalRedeemEventId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
