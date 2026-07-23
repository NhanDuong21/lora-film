package com.lorafilm.booking.booking.dto;

public class BookingPaymentResultResponseDto {
    private String eventId;
    private Boolean applied;
    private Boolean duplicate;
    private String result;

    public BookingPaymentResultResponseDto() {}

    public BookingPaymentResultResponseDto(String eventId, Boolean applied, Boolean duplicate, String result) {
        this.eventId = eventId;
        this.applied = applied;
        this.duplicate = duplicate;
        this.result = result;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Boolean getApplied() { return applied; }
    public void setApplied(Boolean applied) { this.applied = applied; }

    public Boolean getDuplicate() { return duplicate; }
    public void setDuplicate(Boolean duplicate) { this.duplicate = duplicate; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
