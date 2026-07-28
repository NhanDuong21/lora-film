package com.project.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.authservice.entity.OutboxMessage;
import com.project.authservice.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthOutboxService {
    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;

    public AuthOutboxService(OutboxMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, Object aggregateId, Object data) {
        record(eventType, "ACCOUNT", aggregateId, data);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, String aggregateType, Object aggregateId, Object data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", "1.0");
        envelope.put("source", "auth-service");
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("data", data);
        try {
            repository.save(new OutboxMessage(aggregateType, String.valueOf(aggregateId), eventType,
                    objectMapper.writeValueAsString(envelope)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize auth domain event", exception);
        }
    }
}
