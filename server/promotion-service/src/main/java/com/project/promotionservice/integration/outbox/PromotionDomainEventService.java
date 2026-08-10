package com.project.promotionservice.integration.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PromotionDomainEventService {

    private final PromotionOutboxEventRepository repository;
    private final PromotionOutboxEnvelopeFactory envelopeFactory;

    public PromotionDomainEventService(PromotionOutboxEventRepository repository,
                                        PromotionOutboxEnvelopeFactory envelopeFactory) {
        this.repository = repository;
        this.envelopeFactory = envelopeFactory;
    }

    public PromotionOutboxEvent enqueue(String aggregateType, String aggregatePublicId,
                                        String eventType, String topic, Object payload,
                                        String actor) {
        if (aggregateType == null || aggregateType.isBlank()
                || aggregatePublicId == null || aggregatePublicId.isBlank()
                || eventType == null || eventType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER,
                    "Event aggregate and type are required", HttpStatus.BAD_REQUEST);
        }
        PromotionOutboxEvent event = new PromotionOutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregatePublicId(aggregatePublicId);
        event.setEventType(eventType);
        event.setEventKey(aggregatePublicId);
        event.setTopicName(topic);
        event.setPublishStatus(OutboxStatus.PENDING);
        event.setCreatedBy(actor);
        event.setUpdatedBy(actor);
        try {
            event.setPayload(envelopeFactory.create(event, payload));
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Unable to serialize promotion event", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return repository.save(event);
    }
}
