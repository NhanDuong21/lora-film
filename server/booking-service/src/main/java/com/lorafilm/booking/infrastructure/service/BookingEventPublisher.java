package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;

/**
 * Port interface for publishing outbox events.
 */
public interface BookingEventPublisher {
    /**
     * Publishes an outbox event to the message broker.
     * @param event the event to publish
     */
    void publish(BookingOutboxEvent event);
}
