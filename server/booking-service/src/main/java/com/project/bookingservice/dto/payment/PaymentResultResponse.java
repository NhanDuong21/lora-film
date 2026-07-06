package com.project.bookingservice.dto.payment;

public class PaymentResultResponse {

    private String eventId;
    private boolean applied;
    private boolean duplicate;
    private String result;

    public PaymentResultResponse() {
    }

    public PaymentResultResponse(String eventId, boolean applied, boolean duplicate, String result) {
        this.eventId = eventId;
        this.applied = applied;
        this.duplicate = duplicate;
        this.result = result;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isApplied() {
        return applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
