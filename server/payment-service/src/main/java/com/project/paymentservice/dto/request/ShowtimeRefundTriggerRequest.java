package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ShowtimeRefundTriggerRequest {
    @NotBlank
    @Size(max = 180)
    private String eventId;
    @Size(max = 2000)
    private String note;

    public String getEventId() { return eventId; }
    public void setEventId(String value) { this.eventId = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
