package com.project.notificationservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import com.project.notificationservice.entity.NotificationDeadLetter;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationDeliveryAttempt;
import com.project.notificationservice.entity.NotificationOutbox;
import com.project.notificationservice.entity.NotificationRecipient;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.provider.NotificationChannelSender;
import com.project.notificationservice.provider.NotificationChannelSender.DeliveryResult;
import com.project.notificationservice.provider.NotificationChannelSender.RenderedNotification;
import com.project.notificationservice.provider.NotificationSenderResolver;
import com.project.notificationservice.repository.NotificationDeadLetterRepository;
import com.project.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationOutboxRepository;
import com.project.notificationservice.repository.NotificationRecipientRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.template.SafeTemplateRenderer;
import com.project.notificationservice.template.TemplateRegistry;
import com.project.notificationservice.template.TemplatePayloadAdapter;
import com.project.notificationservice.template.TemplateRegistry.RenderedTemplate;
import com.project.notificationservice.template.TemplateRegistry.TemplateDocument;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class NotificationDeliveryWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationDeliveryWorker.class);
    private static final long[] RETRY_DELAYS_SECONDS = {0, 30, 120, 600, 1800};

    private final NotificationRequestRepository requestRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final TemplateRegistry templateRegistry;
    private final SafeTemplateRenderer renderer;
    private final TemplatePayloadAdapter payloadAdapter;
    private final NotificationSenderResolver senderResolver;
    private final RecipientCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final int batchSize;
    private final int maxAttempts;

    public NotificationDeliveryWorker(
            NotificationRequestRepository requestRepository,
            NotificationRecipientRepository recipientRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationDeadLetterRepository deadLetterRepository,
            NotificationOutboxRepository outboxRepository,
            TemplateRegistry templateRegistry,
            SafeTemplateRenderer renderer,
            TemplatePayloadAdapter payloadAdapter,
            NotificationSenderResolver senderResolver,
            RecipientCryptoService cryptoService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${notification.delivery.batch-size:50}") int batchSize,
            @Value("${notification.delivery.max-attempts:5}") int maxAttempts) {
        this.requestRepository = requestRepository;
        this.recipientRepository = recipientRepository;
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.outboxRepository = outboxRepository;
        this.templateRegistry = templateRegistry;
        this.renderer = renderer;
        this.payloadAdapter = payloadAdapter;
        this.senderResolver = senderResolver;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.batchSize = batchSize;
        this.maxAttempts = Math.min(maxAttempts, RETRY_DELAYS_SECONDS.length);
    }

    @Scheduled(fixedDelayString = "${notification.delivery.worker-delay-ms:5000}")
    @Transactional
    public void deliverDueNotifications() {
        List<NotificationDelivery> due = deliveryRepository.findDue(
                EnumSet.of(DeliveryStatus.PENDING, DeliveryStatus.RETRY_SCHEDULED),
                Instant.now(),
                PageRequest.of(0, batchSize));
        for (NotificationDelivery delivery : due) {
            process(delivery);
        }
    }

    private void process(NotificationDelivery delivery) {
        NotificationRequest request = requestRepository.findById(delivery.getNotificationRequestId()).orElse(null);
        NotificationRecipient recipient = recipientRepository.findById(delivery.getNotificationRecipientId()).orElse(null);
        if (request == null || recipient == null) {
            failWithoutRetry(delivery, FailureCategory.PAYLOAD_ERROR,
                    "DELIVERY_RELATION_MISSING", "Request or recipient no longer exists", 0);
            return;
        }
        if (request.getStatus() == RequestStatus.CANCELLED
                || request.getExpiresAt() != null && !request.getExpiresAt().isAfter(Instant.now())) {
            delivery.setStatus(DeliveryStatus.CANCELLED);
            return;
        }
        delivery.setStatus(DeliveryStatus.PROCESSING);
        int attemptNumber = delivery.getAttemptCount() + 1;
        long started = System.nanoTime();
        DeliveryResult result;
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    request.getPayloadJson(), new TypeReference<>() {
                    });
            TemplateDocument template = templateRegistry.getPublishedTemplate(
                    request.getTemplateKey(), delivery.getChannel(), request.getLocale());
            request.setTemplateCommitSha(template.commitSha());
            request.setTemplateVersion(template.version());
            RenderedTemplate rendered = renderer.render(
                    template, payloadAdapter.adapt(payload, template));
            Map<String, Object> providerPayload = new HashMap<>(payload);
            providerPayload.put("_deliveryDatabaseId", delivery.getId());
            NotificationChannelSender sender = senderResolver.resolve(delivery.getChannel());
            result = sender.send(new RenderedNotification(
                    request.getPublicId(),
                    delivery.getPublicId(),
                    recipient.getUserPublicId(),
                    destination(delivery, recipient),
                    rendered.subject(),
                    rendered.htmlContent(),
                    rendered.textContent(),
                    deepLink(payload),
                    request.getCategory().name(),
                    request.getExpiresAt(),
                    Map.copyOf(providerPayload)));
        } catch (NotificationException exception) {
            FailureCategory category = exception.getStatus().is5xxServerError()
                    ? FailureCategory.TRANSIENT : FailureCategory.TEMPLATE_ERROR;
            result = DeliveryResult.failure("template-registry", category,
                    exception.getErrorCode(), exception.getMessage(), null);
        } catch (Exception exception) {
            result = DeliveryResult.failure(delivery.getProvider(), FailureCategory.PAYLOAD_ERROR,
                    "DELIVERY_PREPARATION_FAILED", safeMessage(exception), null);
        }
        long durationMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
        delivery.setAttemptCount(attemptNumber);
        saveAttempt(delivery, result, attemptNumber, durationMs);
        if (result.successful()) {
            handleSuccess(request, delivery, result, durationMs);
        } else {
            handleFailure(request, delivery, result, durationMs);
        }
        updateRequestStatus(request);
    }

    private void handleSuccess(
            NotificationRequest request,
            NotificationDelivery delivery,
            DeliveryResult result,
            long durationMs) {
        Instant now = Instant.now();
        delivery.setProvider(result.provider());
        delivery.setProviderMessageId(result.providerMessageId());
        boolean deliveryConfirmed = delivery.getChannel()
                == com.project.notificationservice.domain.NotificationTypes.Channel.IN_APP;
        delivery.setStatus(deliveryConfirmed ? DeliveryStatus.DELIVERED : DeliveryStatus.SENT);
        delivery.setSentAt(now);
        delivery.setDeliveredAt(deliveryConfirmed ? now : null);
        delivery.setFailureCategory(null);
        delivery.setFailureCode(null);
        delivery.setFailureMessage(null);
        delivery.setNextRetryAt(null);
        outbox(request, delivery, "notification.sent");
        meterRegistry.counter("notification_delivery_success_total",
                "channel", delivery.getChannel().name(),
                "provider", result.provider()).increment();
        meterRegistry.timer("notification_provider_duration",
                "channel", delivery.getChannel().name(),
                "provider", result.provider()).record(Duration.ofMillis(durationMs));
        LOGGER.info("Notification delivered notificationPublicId={} deliveryPublicId={} eventType={} "
                        + "correlationId={} templateKey={} templateCommitSha={} templateVersion={} "
                        + "channel={} provider={} status={} attemptNumber={} durationMs={}",
                request.getPublicId(), delivery.getPublicId(), request.getEventType(),
                request.getCorrelationId(), request.getTemplateKey(), request.getTemplateCommitSha(),
                request.getTemplateVersion(), delivery.getChannel(), result.provider(),
                delivery.getStatus(), delivery.getAttemptCount(), durationMs);
    }

    private void handleFailure(
            NotificationRequest request,
            NotificationDelivery delivery,
            DeliveryResult result,
            long durationMs) {
        delivery.setProvider(result.provider());
        delivery.setFailureCategory(result.failureCategory());
        delivery.setFailureCode(result.failureCode());
        delivery.setFailureMessage(truncate(result.failureMessage(), 500));
        boolean retryable = result.failureCategory() == FailureCategory.TRANSIENT
                || result.failureCategory() == FailureCategory.RATE_LIMITED;
        if (retryable && delivery.getAttemptCount() < maxAttempts) {
            long delay = result.retryAfterSeconds() == null
                    ? RETRY_DELAYS_SECONDS[delivery.getAttemptCount()]
                    : Math.max(result.retryAfterSeconds(), RETRY_DELAYS_SECONDS[delivery.getAttemptCount()]);
            long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(2, delay / 10 + 1));
            delivery.setStatus(DeliveryStatus.RETRY_SCHEDULED);
            delivery.setNextRetryAt(Instant.now().plusSeconds(delay + jitter));
            meterRegistry.counter("notification_retry_total",
                    "channel", delivery.getChannel().name(),
                    "failureCategory", result.failureCategory().name()).increment();
        } else if (retryable) {
            deadLetter(delivery, result);
        } else {
            delivery.setStatus(DeliveryStatus.FAILED);
            delivery.setFailedAt(Instant.now());
            delivery.setNextRetryAt(null);
        }
        outbox(request, delivery,
                delivery.getStatus() == DeliveryStatus.DEAD_LETTERED
                        ? "notification.dead-lettered" : "notification.failed");
        meterRegistry.counter("notification_delivery_failure_total",
                "channel", delivery.getChannel().name(),
                "failureCategory", result.failureCategory().name()).increment();
        LOGGER.warn("Notification delivery failed notificationPublicId={} deliveryPublicId={} eventType={} "
                        + "correlationId={} templateKey={} channel={} provider={} status={} "
                        + "failureCategory={} failureCode={} attemptNumber={} durationMs={}",
                request.getPublicId(), delivery.getPublicId(), request.getEventType(),
                request.getCorrelationId(), request.getTemplateKey(), delivery.getChannel(),
                result.provider(), delivery.getStatus(), result.failureCategory(), result.failureCode(),
                delivery.getAttemptCount(), durationMs);
    }

    private void deadLetter(NotificationDelivery delivery, DeliveryResult result) {
        delivery.setStatus(DeliveryStatus.DEAD_LETTERED);
        delivery.setFailedAt(Instant.now());
        delivery.setNextRetryAt(null);
        NotificationDeadLetter deadLetter = deadLetterRepository
                .findByNotificationDeliveryId(delivery.getId()).orElseGet(NotificationDeadLetter::new);
        deadLetter.setNotificationDeliveryId(delivery.getId());
        deadLetter.setReason(result.failureCode() == null ? "RETRY_EXHAUSTED" : result.failureCode());
        deadLetter.setFailureMessage(truncate(result.failureMessage(), 500));
        deadLetterRepository.save(deadLetter);
        meterRegistry.counter("notification_dead_letter_total",
                "channel", delivery.getChannel().name()).increment();
    }

    private void failWithoutRetry(
            NotificationDelivery delivery,
            FailureCategory category,
            String code,
            String message,
            long durationMs) {
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureCategory(category);
        delivery.setFailureCode(code);
        delivery.setFailureMessage(message);
        delivery.setFailedAt(Instant.now());
        DeliveryResult result = DeliveryResult.failure(delivery.getProvider(), category, code, message, null);
        saveAttempt(delivery, result, delivery.getAttemptCount(), durationMs);
    }

    private void saveAttempt(
            NotificationDelivery delivery,
            DeliveryResult result,
            int attemptNumber,
            long durationMs) {
        NotificationDeliveryAttempt attempt = new NotificationDeliveryAttempt();
        attempt.setNotificationDeliveryId(delivery.getId());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setProvider(result.provider());
        attempt.setOutcome(result.successful() ? "SUCCESS" : "FAILURE");
        attempt.setFailureCategory(result.failureCategory());
        attempt.setFailureCode(result.failureCode());
        attempt.setFailureMessage(truncate(result.failureMessage(), 500));
        attempt.setDurationMs(durationMs);
        attemptRepository.save(attempt);
        meterRegistry.counter("notification_deliveries_total",
                "channel", delivery.getChannel().name(),
                "outcome", attempt.getOutcome()).increment();
    }

    private void updateRequestStatus(NotificationRequest request) {
        List<DeliveryStatus> statuses = deliveryRepository
                .findByNotificationRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .map(NotificationDelivery::getStatus)
                .toList();
        if (statuses.stream().anyMatch(status -> status == DeliveryStatus.PENDING
                || status == DeliveryStatus.PROCESSING || status == DeliveryStatus.RETRY_SCHEDULED)) {
            request.setStatus(RequestStatus.PROCESSING);
        } else if (statuses.stream().allMatch(status -> status == DeliveryStatus.SENT
                || status == DeliveryStatus.DELIVERED || status == DeliveryStatus.SUPPRESSED)) {
            request.setStatus(RequestStatus.COMPLETED);
        } else if (statuses.stream().anyMatch(status -> status == DeliveryStatus.SENT
                || status == DeliveryStatus.DELIVERED)) {
            request.setStatus(RequestStatus.PARTIALLY_FAILED);
        } else {
            request.setStatus(RequestStatus.FAILED);
        }
    }

    private void outbox(
            NotificationRequest request, NotificationDelivery delivery, String eventType) {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setAggregatePublicId(delivery.getPublicId());
        outbox.setEventType(eventType);
        try {
            outbox.setPayloadJson(objectMapper.writeValueAsString(Map.of(
                    "notificationPublicId", request.getPublicId(),
                    "deliveryPublicId", delivery.getPublicId(),
                    "channel", delivery.getChannel().name(),
                    "status", delivery.getStatus().name(),
                    "occurredAt", Instant.now().toString())));
        } catch (Exception exception) {
            outbox.setPayloadJson("{}");
        }
        outboxRepository.save(outbox);
    }

    private String destination(
            NotificationDelivery delivery,
            NotificationRecipient recipient) {
        return switch (delivery.getChannel()) {
            case EMAIL -> cryptoService.decrypt(recipient.getEmailEncrypted());
            case SMS -> cryptoService.decrypt(recipient.getPhoneEncrypted());
            case IN_APP -> recipient.getUserPublicId();
            case WEB_PUSH -> cryptoService.decrypt(recipient.getWebPushSubscriptionEncrypted());
        };
    }

    private String deepLink(Map<String, Object> payload) {
        Object value = payload.getOrDefault("deepLink", payload.get("ticketAccessUrl"));
        return value == null ? null : String.valueOf(value);
    }

    private String truncate(String value, int maximum) {
        if (value == null || value.length() <= maximum) return value;
        return value.substring(0, maximum);
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
