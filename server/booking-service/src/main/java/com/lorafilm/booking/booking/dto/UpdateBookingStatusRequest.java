package com.lorafilm.booking.booking.dto;

import com.lorafilm.booking.booking.enums.BookingStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateBookingStatusRequest {

    @NotNull(message = "Status is required")
    private BookingStatus status;

    private String reason;

    private String source;

    private String note;

    public UpdateBookingStatusRequest() {
    }

    public UpdateBookingStatusRequest(BookingStatus status, String reason, String source, String note) {
        this.status = status;
        this.reason = reason;
        this.source = source;
        this.note = note;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
