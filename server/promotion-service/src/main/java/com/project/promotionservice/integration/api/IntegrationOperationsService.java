package com.project.promotionservice.integration.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.promotionservice.common.exception.BusinessException;
import com.project.promotionservice.common.exception.ErrorCode;
import com.project.promotionservice.integration.inbox.*;
import com.project.promotionservice.integration.outbox.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

@Service
public class IntegrationOperationsService {
    private final PromotionDomainEventService events;
    private final PromotionOutboxEventRepository outbox;
    private final PromotionIntegrationEventRepository inbox;
    private final IntegrationEventService integrationEvents;
    private final ObjectMapper objectMapper;

    public IntegrationOperationsService(PromotionDomainEventService events,
                                        PromotionOutboxEventRepository outbox,
                                        PromotionIntegrationEventRepository inbox,
                                        IntegrationEventService integrationEvents,
                                        ObjectMapper objectMapper) {
        this.events = events;
        this.outbox = outbox;
        this.inbox = inbox;
        this.integrationEvents = integrationEvents;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventHistoryResponse publish(EventPublishRequest request, String actor) {
        if (request.getTopicName() == null
                || !request.getTopicName().toLowerCase(java.util.Locale.ROOT).startsWith("promotion.")) {
            throw bad("Only promotion-owned Kafka topics can be published by this service");
        }
        com.fasterxml.jackson.databind.JsonNode payload;
        try {
            payload = objectMapper.readTree(request.getPayloadJson());
        } catch (Exception ex) {
            throw bad("payloadJson must be valid JSON");
        }
        PromotionOutboxEvent event = events.enqueue(request.getAggregateType().toUpperCase(),
                request.getAggregatePublicId(), request.getEventType().toUpperCase(),
                request.getTopicName(), payload, actor);
        return outbound(event);
    }

    @Transactional
    public void retry(String publicId, String actor) {
        PromotionOutboxEvent event = outbox.findByPublicId(publicId)
                .orElseThrow(() -> notFound("Outbound event not found"));
        if (event.getPublishStatus() != OutboxStatus.FAILED
                && event.getPublishStatus() != OutboxStatus.DEAD_LETTER) {
            throw bad("Only failed or dead-letter events can be retried");
        }
        event.setPublishStatus(OutboxStatus.PENDING);
        event.setNextRetryAt(Instant.now());
        event.setErrorMessage(null);
        event.setProcessingOwner(null);
        event.setProcessingStartedAt(null);
        outbox.save(event);
    }

    @Transactional(readOnly = true)
    public EventStatusResponse status() {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        for (OutboxStatus value : OutboxStatus.values()) out.put(value.name(), outbox.countByPublishStatus(value));
        Map<String, Long> in = new java.util.LinkedHashMap<>();
        for (IntegrationEventStatus value : IntegrationEventStatus.values()) in.put(value.name(), inbox.countByProcessingStatus(value));
        return new EventStatusResponse(out, in);
    }

    @Transactional
    public void reprocess(EventReprocessRequest request) {
        if (inbox.findByPublicId(request.getEventPublicId()).isPresent()) {
            integrationEvents.reprocess(request.getEventPublicId());
            return;
        }
        retry(request.getEventPublicId(), "OPERATIONS_SERVICE");
    }

    @Transactional(readOnly = true)
    public List<EventHistoryResponse> history(String direction, int page, int size) {
        String normalized = direction == null ? "OUTBOUND" : direction.toUpperCase();
        if ("INBOUND".equals(normalized)) {
            return inbox.findByProcessingStatusIn(Arrays.asList(IntegrationEventStatus.values()),
                            PageRequest.of(page, size)).stream().map(this::inbound).toList();
        }
        return outbox.findByPublishStatusIn(Arrays.asList(OutboxStatus.values()), PageRequest.of(page, size))
                .stream().map(this::outbound).toList();
    }

    @Transactional(readOnly = true)
    public EventHistoryResponse detail(String publicId, String direction) {
        if ("INBOUND".equalsIgnoreCase(direction)) {
            return inbox.findByPublicId(publicId).map(this::inbound)
                    .orElseThrow(() -> notFound("Inbound event not found"));
        }
        return outbox.findByPublicId(publicId).map(this::outbound)
                .orElseThrow(() -> notFound("Outbound event not found"));
    }

    private EventHistoryResponse outbound(PromotionOutboxEvent e) {
        EventHistoryResponse r = new EventHistoryResponse();
        r.setPublicId(e.getPublicId()); r.setDirection("OUTBOUND");
        r.setAggregateType(e.getAggregateType()); r.setAggregatePublicId(e.getAggregatePublicId());
        r.setEventType(e.getEventType()); r.setTopicName(e.getTopicName());
        r.setStatus(e.getPublishStatus().name()); r.setRetryCount(e.getRetryCount());
        r.setErrorMessage(e.getErrorMessage()); r.setCreatedAt(e.getCreatedAt());
        r.setPublishedAt(e.getPublishedAt()); r.setPayload(e.getPayload());
        r.setEventId(e.getPublicId()); r.setSchemaVersion("1.0");
        return r;
    }

    private EventHistoryResponse inbound(PromotionIntegrationEvent e) {
        EventHistoryResponse r = new EventHistoryResponse();
        r.setPublicId(e.getPublicId()); r.setDirection("INBOUND");
        r.setSourceService(e.getSourceService()); r.setEventId(e.getEventId());
        r.setEventType(e.getEventType()); r.setSchemaVersion(e.getSchemaVersion());
        r.setStatus(e.getProcessingStatus().name()); r.setRetryCount(e.getRetryCount());
        r.setErrorMessage(e.getLastError()); r.setCreatedAt(e.getCreatedAt());
        r.setProcessedAt(e.getProcessedAt()); r.setPayload(e.getPayload());
        return r;
    }

    private static BusinessException bad(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER, message, HttpStatus.BAD_REQUEST);
    }
    private static BusinessException notFound(String message) {
        return new BusinessException(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
