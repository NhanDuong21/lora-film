package com.lorafilm.booking.event.consumer;

public interface EventConsumer {
    void consume(String payload);
}
