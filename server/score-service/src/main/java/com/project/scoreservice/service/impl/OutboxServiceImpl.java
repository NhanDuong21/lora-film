package com.project.scoreservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.scoreservice.entity.OutboxEvent;
import com.project.scoreservice.repository.OutboxEventRepository;
import com.project.scoreservice.service.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxServiceImpl implements OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxServiceImpl.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxServiceImpl(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, Object payload, String correlationId) {
        String payloadJson;
        if (payload instanceof String str) {
            payloadJson = str;
        } else {
            try {
                payloadJson = objectMapper.writeValueAsString(payload);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox payload for eventType {}: {}", eventType, e.getMessage());
                payloadJson = "{}";
            }
        }

        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, payloadJson, correlationId);
        OutboxEvent saved = outboxEventRepository.save(event);
        log.debug("Saved outbox event [{}] for aggregate [{}:{}] in active transaction",
                saved.getEventId(), aggregateType, aggregateId);
        return saved;
    }
}
