package com.lorafilm.booking.infrastructure.service.impl;

import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.service.BookingEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockBookingEventPublisher implements BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MockBookingEventPublisher.class);

    @Override
    public void publish(BookingOutboxEvent event) {
        log.info("[MOCK PUBLISH] Event ID: {}, Type: {}, Aggregate: {} [ID: {}], Payload: {}",
                event.getEventId(), event.getEventType(), event.getAggregateType(), event.getAggregateId(), event.getPayload());
        // Simulating random network errors to test retry logic
        if (event.getEventType().contains("FAIL_TRIGGER_TEST")) {
            throw new RuntimeException("Simulated broker connection failure");
        }
    }
}
