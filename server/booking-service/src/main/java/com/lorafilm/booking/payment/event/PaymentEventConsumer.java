package com.lorafilm.booking.payment.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.booking.common.exception.BusinessException;
import com.lorafilm.booking.infrastructure.entity.BookingInboxEvent;
import com.lorafilm.booking.infrastructure.entity.BookingRetryTask;
import com.lorafilm.booking.infrastructure.enums.RetryTaskStatus;
import com.lorafilm.booking.infrastructure.enums.RetryTaskType;
import com.lorafilm.booking.infrastructure.repository.BookingInboxEventRepository;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.payment.event.contract.PaymentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final BookingInboxEventRepository inboxRepository;
    private final BookingRetryTaskRepository retryTaskRepository;
    private final PaymentEventProcessor processor;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(
            BookingInboxEventRepository inboxRepository,
            BookingRetryTaskRepository retryTaskRepository,
            PaymentEventProcessor processor,
            ObjectMapper objectMapper) {
        this.inboxRepository = inboxRepository;
        this.retryTaskRepository = retryTaskRepository;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    public void consume(String eventJson) {
        PaymentEvent event;
        try {
            event = objectMapper.readValue(eventJson, PaymentEvent.class);
        } catch (Exception e) {
            log.error("Failed to parse PaymentEvent JSON. Non-retryable. Payload: {}", eventJson, e);
            return;
        }

        if (event == null || event.eventId() == null || event.eventType() == null) {
            log.error("Invalid PaymentEvent: missing eventId or eventType. Envelope: {}", event);
            return;
        }

        // 1. Idempotency Check & Inbox Persistence
        Optional<BookingInboxEvent> existingInbox = inboxRepository.findByEventId(event.eventId());
        BookingInboxEvent inboxRecord;

        if (existingInbox.isPresent()) {
            inboxRecord = existingInbox.get();
            if (Boolean.TRUE.equals(inboxRecord.getProcessed())) {
                log.info("Duplicate event detected and already processed. eventId: {}. Skipping.", event.eventId());
                return;
            }
        } else {
            inboxRecord = new BookingInboxEvent();
            inboxRecord.setEventId(event.eventId());
            inboxRecord.setSourceService(event.producer() != null ? event.producer() : "payment-service");
            inboxRecord.setEventType(event.eventType());
            inboxRecord.setPayload(eventJson);
            inboxRecord.setProcessed(false);
            if (event.aggregateId() != null) {
                try {
                    inboxRecord.setAggregateId(Long.parseLong(event.aggregateId()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            inboxRecord.setAggregateType(event.aggregateType());
            inboxRecord = inboxRepository.save(inboxRecord);
        }

        try {
            // Validate event
            validateEvent(event);

            // 2. Delegate to transactional processor
            processor.process(event, eventJson);

            // 3. Mark Inbox as Processed
            inboxRecord.setProcessed(true);
            inboxRecord.setProcessedAt(Instant.now());
            inboxRecord.setErrorMessage(null);
            inboxRepository.save(inboxRecord);
            log.info("Successfully processed PaymentEvent eventId: {}", event.eventId());

        } catch (BusinessException e) {
            log.error("Business validation failure for eventId: {}. Non-retryable.", event.eventId(), e);
            inboxRecord.setProcessed(true);
            inboxRecord.setErrorMessage("BUSINESS_FAIL: " + e.getMessage());
            inboxRepository.save(inboxRecord);

        } catch (IllegalArgumentException e) {
            log.error("Validation failure for eventId: {}. Non-retryable.", event.eventId(), e);
            inboxRecord.setProcessed(true);
            inboxRecord.setErrorMessage("VALIDATION_FAIL: " + e.getMessage());
            inboxRepository.save(inboxRecord);

        } catch (TransientDataAccessException e) {
            log.warn("Transient database error while processing eventId: {}. Registering retry task.", event.eventId(), e);
            inboxRecord.setErrorMessage("TRANSIENT_ERROR: " + e.getMessage());
            inboxRepository.save(inboxRecord);
            registerRetryTask(inboxRecord, e.getMessage());
            throw e;


        } catch (Exception e) {
            log.error("Unexpected error while processing eventId: {}", event.eventId(), e);
            inboxRecord.setErrorMessage("UNEXPECTED_ERROR: " + e.getMessage());
            inboxRepository.save(inboxRecord);
            registerRetryTask(inboxRecord, e.getMessage());
            throw e;
        }
    }

    private void validateEvent(PaymentEvent event) {
        if (event.payload() == null) {
            throw new IllegalArgumentException("Payment event payload is missing.");
        }
        if (event.payload().bookingPublicId() == null && event.payload().bookingId() == null) {
            throw new IllegalArgumentException("Booking public ID is missing in payload.");
        }
        if (event.payload().amount() == null) {
            throw new IllegalArgumentException("Amount is missing in payload.");
        }
    }

    private void registerRetryTask(BookingInboxEvent inboxRecord, String errorMsg) {
        try {
            boolean exists = retryTaskRepository.existsByTaskTypeAndReferenceId(
                    RetryTaskType.INBOX_PROCESS, inboxRecord.getId());
            if (!exists) {
                BookingRetryTask retryTask = new BookingRetryTask();
                retryTask.setPublicId(UUID.randomUUID().toString());
                retryTask.setTaskType(RetryTaskType.INBOX_PROCESS);
                retryTask.setReferenceType("InboxEvent");
                retryTask.setReferenceId(inboxRecord.getId());
                retryTask.setPayload(inboxRecord.getPayload());
                retryTask.setRetryCount(0);
                retryTask.setMaxRetry(5);
                retryTask.setStatus(RetryTaskStatus.PENDING);
                retryTask.setNextRetryAt(Instant.now().plusSeconds(30));
                retryTask.setErrorCode("TRANSIENT_ERROR");
                retryTask.setErrorMessage(errorMsg);
                retryTaskRepository.save(retryTask);
                log.debug("Registered retry task for InboxEvent ID: {}", inboxRecord.getId());
            }
        } catch (Exception ex) {
            log.error("Failed to register retry task for InboxEvent ID: {}", inboxRecord.getId(), ex);
        }
    }
}
