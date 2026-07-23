package com.lorafilm.movie.showtime.dto.response;

public class BatchStatusReasonGroup {

    private String reasonCode;
    private String reason;
    private int count;

    public BatchStatusReasonGroup() {}

    public BatchStatusReasonGroup(String reasonCode, String reason, int count) {
        this.reasonCode = reasonCode;
        this.reason = reason;
        this.count = count;
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

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
