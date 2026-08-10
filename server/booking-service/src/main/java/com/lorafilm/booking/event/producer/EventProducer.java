package com.lorafilm.booking.event.producer;

public interface EventProducer {
    void publish(String topic, Object event);
}
