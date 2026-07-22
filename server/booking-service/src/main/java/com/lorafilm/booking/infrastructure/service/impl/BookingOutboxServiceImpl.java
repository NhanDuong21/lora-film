package com.lorafilm.booking.infrastructure.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.entity.BookingOutboxEvent;
import com.lorafilm.booking.infrastructure.enums.OutboxStatus;
import com.lorafilm.booking.infrastructure.repository.BookingOutboxEventRepository;
import com.lorafilm.booking.infrastructure.service.BookingOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookingOutboxServiceImpl implements BookingOutboxService {

    private final BookingOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BookingOutboxServiceImpl(BookingOutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public BookingOutboxEvent createOutboxEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        if (aggregateType == null || aggregateType.trim().isEmpty()) {
            throw new BusinessException("INVALID_OUTBOX_DATA", "Aggregate type cannot be empty");
        }
        if (aggregateId == null) {
            throw new BusinessException("INVALID_OUTBOX_DATA", "Aggregate ID cannot be null");
        }
        if (eventType == null || eventType.trim().isEmpty()) {
            throw new BusinessException("INVALID_OUTBOX_DATA", "Event type cannot be empty");
        }

        String jsonPayload;
        if (payload instanceof String) {
            jsonPayload = (String) payload;
        } else {
            try {
                jsonPayload = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                jsonPayload = "{\"aggregateId\":" + aggregateId + ",\"eventType\":\"" + eventType + "\"}";
            }
        }

        BookingOutboxEvent event = new BookingOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setEventVersion(1);
        event.setPayload(jsonPayload);
        event.setStatus(OutboxStatus.PENDING);
        event.setRetryCount(0);

        return outboxEventRepository.save(event);
    }
}
