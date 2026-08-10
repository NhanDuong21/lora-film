package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;

public interface BookingOutboxService {

    BookingOutboxEvent createOutboxEvent(String aggregateType, Long aggregateId, String eventType, Object payload);
}
