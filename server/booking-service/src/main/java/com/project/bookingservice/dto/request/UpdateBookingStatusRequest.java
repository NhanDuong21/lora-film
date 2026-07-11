package com.project.bookingservice.dto.request;

import com.project.bookingservice.enumtype.BookingStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateBookingStatusRequest {
    
    @NotNull(message = "New status is required")
    private BookingStatus newStatus;
    
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    public UpdateBookingStatusRequest() {
    }

    public UpdateBookingStatusRequest(BookingStatus newStatus, String reason) {
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public BookingStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(BookingStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
