package com.project.scoreservice.dto;
 
import jakarta.validation.constraints.NotNull;
 
public class RedeemPreviewRequest {
    @NotNull(message = "Booking ID is required")
    private Long bookingId;
 
    @NotNull(message = "Requested points must be specified")
    private Integer requestedPoints;
 
    public RedeemPreviewRequest() {
    }
 
    public RedeemPreviewRequest(Long bookingId, Integer requestedPoints) {
        this.bookingId = bookingId;
        this.requestedPoints = requestedPoints;
    }
 
    public Long getBookingId() {
        return bookingId;
    }
 
    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
 
    public Integer getRequestedPoints() {
        return requestedPoints;
    }
 
    public void setRequestedPoints(Integer requestedPoints) {
        this.requestedPoints = requestedPoints;
    }
}
