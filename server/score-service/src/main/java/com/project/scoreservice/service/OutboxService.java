package com.project.scoreservice.service;

import com.project.scoreservice.entity.OutboxEvent;

public interface OutboxService {
    OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, Object payload, String correlationId);
}
