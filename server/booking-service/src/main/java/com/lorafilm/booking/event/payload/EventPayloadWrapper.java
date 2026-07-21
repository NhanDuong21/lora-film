package com.lorafilm.booking.event.payload;

import java.time.Instant;

public class EventPayloadWrapper<T> {

    private String eventType;
    private T payload;
    private Instant timestamp;

    public EventPayloadWrapper() {
        this.timestamp = Instant.now();
    }

    public EventPayloadWrapper(String eventType, T payload) {
        this.eventType = eventType;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
