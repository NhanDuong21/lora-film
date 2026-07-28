package com.project.promotionservice.benefit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.integration.outbox.OutboxStatus;
import com.project.promotionservice.integration.outbox.PromotionOutboxEvent;
import com.project.promotionservice.integration.outbox.PromotionOutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BenefitEventService {

    private static final String BENEFIT_TOPIC = "promotion.benefit.lifecycle";

    private final PromotionOutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BenefitEventService(PromotionOutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public void record(String aggregateType, String aggregatePublicId,
                       String eventType, Object payload, String actor) {
        PromotionOutboxEvent event = new PromotionOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregatePublicId(aggregatePublicId);
        event.setEventType(eventType);
        event.setEventKey(aggregatePublicId);
        event.setTopicName(BENEFIT_TOPIC);
        event.setPublishStatus(OutboxStatus.PENDING);
        event.setCreatedBy(actor);
        event.setUpdatedBy(actor);
        try {
            event.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unable to serialize benefit event",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        outboxEventRepository.save(event);
    }
}
