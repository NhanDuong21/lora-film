package com.lorafilm.movie.showtime.dto.response;

public class BatchStatusBlockedShowtime {

    private String showtimePublicId;
    private String reasonCode;
    private String reason;

    public BatchStatusBlockedShowtime() {
    }

    public BatchStatusBlockedShowtime(String showtimePublicId, String reasonCode, String reason) {
        this.showtimePublicId = showtimePublicId;
        this.reasonCode = reasonCode;
        this.reason = reason;
    }

    public String getShowtimePublicId() {
        return showtimePublicId;
    }

    public void setShowtimePublicId(String showtimePublicId) {
        this.showtimePublicId = showtimePublicId;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
