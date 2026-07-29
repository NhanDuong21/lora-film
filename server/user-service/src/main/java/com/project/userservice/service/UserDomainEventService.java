package com.project.userservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.userservice.entity.OutboxMessage;
import com.project.userservice.repository.OutboxMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class UserDomainEventService {
    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;

    public UserDomainEventService(OutboxMessageRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String eventType, String aggregateType, Object aggregateId, Object data) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventId", UUID.randomUUID().toString());
        envelope.put("eventType", eventType);
        envelope.put("eventVersion", "1.0");
        envelope.put("source", "user-service");
        envelope.put("occurredAt", Instant.now().toString());
        envelope.put("data", data);
        try {
            repository.save(new OutboxMessage(aggregateType, String.valueOf(aggregateId), eventType,
                    objectMapper.writeValueAsString(envelope)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize domain event " + eventType, exception);
        }
    }
}
