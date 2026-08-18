package com.project.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.notificationservice.domain.NotificationTypes.Category;
import com.project.notificationservice.domain.NotificationTypes.Channel;
import com.project.notificationservice.domain.NotificationTypes.DeliveryStatus;
import com.project.notificationservice.domain.NotificationTypes.FailureCategory;
import com.project.notificationservice.domain.NotificationTypes.Priority;
import com.project.notificationservice.domain.NotificationTypes.RequestStatus;
import com.project.notificationservice.entity.NotificationDelivery;
import com.project.notificationservice.entity.NotificationDeliveryAttempt;
import com.project.notificationservice.entity.NotificationDeadLetter;
import com.project.notificationservice.entity.InAppNotification;
import com.project.notificationservice.entity.NotificationPreference;
import com.project.notificationservice.entity.NotificationRecipient;
import com.project.notificationservice.entity.NotificationRequest;
import com.project.notificationservice.exception.NotificationException;
import com.project.notificationservice.repository.NotificationDeliveryRepository;
import com.project.notificationservice.repository.NotificationDeliveryAttemptRepository;
import com.project.notificationservice.repository.NotificationDeadLetterRepository;
import com.project.notificationservice.repository.InAppNotificationRepository;
import com.project.notificationservice.repository.NotificationPreferenceRepository;
import com.project.notificationservice.repository.NotificationRecipientRepository;
import com.project.notificationservice.repository.NotificationRequestRepository;
import com.project.notificationservice.service.NotificationCommands.AcceptedNotification;
import com.project.notificationservice.service.NotificationCommands.CreateNotificationCommand;
import com.project.notificationservice.service.NotificationCommands.VoucherGrantedNotification;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationApplicationService {

    private final NotificationRequestRepository requestRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationDeliveryAttemptRepository attemptRepository;
    private final NotificationDeadLetterRepository deadLetterRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final RecipientCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public NotificationApplicationService(
            NotificationRequestRepository requestRepository,
            NotificationRecipientRepository recipientRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationDeliveryAttemptRepository attemptRepository,
            NotificationDeadLetterRepository deadLetterRepository,
            NotificationPreferenceRepository preferenceRepository,
            InAppNotificationRepository inAppNotificationRepository,
            RecipientCryptoService cryptoService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.requestRepository = requestRepository;
        this.recipientRepository = recipientRepository;
        this.deliveryRepository = deliveryRepository;
        this.attemptRepository = attemptRepository;
        this.deadLetterRepository = deadLetterRepository;
        this.preferenceRepository = preferenceRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public AcceptedNotification accept(CreateNotificationCommand command) {
        Optional<NotificationRequest> existing =
                requestRepository.findByIdempotencyKey(command.idempotencyKey());
        if (existing.isPresent()) {
            NotificationRequest request = existing.get();
            int deliveries = deliveryRepository
                    .findByNotificationRequestIdOrderByCreatedAtAsc(request.getId()).size();
            return new AcceptedNotification(request.getPublicId(), request.getStatus().name(), true, deliveries);
        }
        validateRecipient(command);
        NotificationRequest request = new NotificationRequest();
        request.setIdempotencyKey(command.idempotencyKey());
        request.setSourceService(command.sourceService());
        request.setSourceEventId(command.sourceEventId());
        request.setEventType(command.eventType());
        request.setCorrelationId(command.correlationId());
        request.setCausationId(command.causationId());
        request.setTemplateKey(command.templateKey());
        request.setLocale(command.locale());
        request.setCategory(command.category());
        request.setPriority(command.priority());
        request.setScheduledAt(command.scheduledAt());
        request.setExpiresAt(command.expiresAt());
        request.setStatus(RequestStatus.ACCEPTED);
        request.setTest(command.test());
        request.setPayloadJson(writeJson(command.payload()));
        request = requestRepository.save(request);

        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setNotificationRequestId(request.getId());
        recipient.setUserPublicId(command.recipient().userPublicId());
        recipient.setEmailEncrypted(cryptoService.encrypt(command.recipient().email()));
        recipient.setPhoneEncrypted(cryptoService.encrypt(command.recipient().phone()));
        recipient.setWebPushSubscriptionEncrypted(
                cryptoService.encrypt(command.recipient().webPushSubscription()));
        recipient.setLocale(command.locale());
        recipient = recipientRepository.save(recipient);

        int count = 0;
        for (Channel channel : command.channels()) {
            NotificationDelivery delivery = new NotificationDelivery();
            delivery.setNotificationRequestId(request.getId());
            delivery.setNotificationRecipientId(recipient.getId());
            delivery.setChannel(channel);
            delivery.setProvider(providerName(channel));
            if (isMarketingSuppressed(command, channel)) {
                delivery.setStatus(DeliveryStatus.SUPPRESSED);
                delivery.setFailureCode("MARKETING_OPT_OUT");
                delivery.setFailureMessage("Customer marketing preference disabled this channel");
            } else {
                delivery.setStatus(DeliveryStatus.PENDING);
                delivery.setNextRetryAt(command.scheduledAt());
            }
            deliveryRepository.save(delivery);
            count++;
        }
        meterRegistry.counter("notification_requests_total",
                "eventType", command.eventType(), "category", command.category().name()).increment();
        return new AcceptedNotification(request.getPublicId(), request.getStatus().name(), false, count);
    }

    @Transactional
    public AcceptedNotification acceptVoucherGranted(VoucherGrantedNotification command) {
        String idempotencyKey = "voucher-granted-" + command.sourceEventId();
        Optional<NotificationRequest> existing = requestRepository
                .findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            NotificationRequest request = existing.get();
            int deliveries = deliveryRepository
                    .findByNotificationRequestIdOrderByCreatedAtAsc(request.getId()).size();
            return new AcceptedNotification(request.getPublicId(), request.getStatus().name(),
                    true, deliveries);
        }

        Instant now = Instant.now();
        boolean emailSuppressed = preferenceRepository
                .findByUserPublicIdAndChannelAndCategory(
                        command.userPublicId(), Channel.EMAIL, Category.MARKETING.name())
                .map(preference -> !preference.isEnabled())
                .orElse(false);
        Map<String, Object> publicData = Map.of(
                "voucherCode", command.voucherCode(),
                "voucherName", command.voucherName());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userName", command.userName());
        payload.put("voucherCode", command.voucherCode());
        payload.put("voucherName", command.voucherName());
        payload.put("discountValue", command.discountValue());
        payload.put("minOrderAmount", command.minimumOrderAmount());
        payload.put("expiryDate", command.expiryDate());
        payload.put("useNowLink", command.useNowLink());
        payload.put("deepLink", command.deepLink());
        payload.put("publicData", publicData);

        NotificationRequest request = new NotificationRequest();
        request.setIdempotencyKey(idempotencyKey);
        request.setSourceService("promotion-service");
        request.setSourceEventId(command.sourceEventId());
        request.setEventType("VOUCHER_GRANTED");
        request.setTemplateKey("VOUCHER_GRANTED");
        request.setLocale("vi-VN");
        request.setCategory(Category.MARKETING);
        request.setPriority(Priority.NORMAL);
        request.setExpiresAt(command.expiresAt());
        request.setStatus(emailSuppressed ? RequestStatus.COMPLETED : RequestStatus.ACCEPTED);
        request.setPayloadJson(writeJson(payload));
        request = requestRepository.save(request);

        NotificationRecipient recipient = new NotificationRecipient();
        recipient.setNotificationRequestId(request.getId());
        recipient.setUserPublicId(command.userPublicId());
        recipient.setEmailEncrypted(cryptoService.encrypt(command.email()));
        recipient.setLocale("vi-VN");
        recipient = recipientRepository.save(recipient);

        NotificationDelivery emailDelivery = new NotificationDelivery();
        emailDelivery.setNotificationRequestId(request.getId());
        emailDelivery.setNotificationRecipientId(recipient.getId());
        emailDelivery.setChannel(Channel.EMAIL);
        emailDelivery.setProvider("smtp");
        if (emailSuppressed) {
            emailDelivery.setStatus(DeliveryStatus.SUPPRESSED);
            emailDelivery.setFailureCode("MARKETING_OPT_OUT");
            emailDelivery.setFailureMessage(
                    "Customer marketing preference disabled this channel");
        } else {
            emailDelivery.setStatus(DeliveryStatus.PENDING);
        }
        deliveryRepository.save(emailDelivery);

        NotificationDelivery inAppDelivery = new NotificationDelivery();
        inAppDelivery.setNotificationRequestId(request.getId());
        inAppDelivery.setNotificationRecipientId(recipient.getId());
        inAppDelivery.setChannel(Channel.IN_APP);
        inAppDelivery.setProvider("in-app");
        inAppDelivery.setStatus(DeliveryStatus.DELIVERED);
        inAppDelivery.setAttemptCount(1);
        inAppDelivery.setSentAt(now);
        inAppDelivery.setDeliveredAt(now);
        inAppDelivery = deliveryRepository.save(inAppDelivery);

        InAppNotification inApp = new InAppNotification();
        inApp.setNotificationDeliveryId(inAppDelivery.getId());
        inApp.setUserPublicId(command.userPublicId());
        inApp.setTitle("Bạn có voucher mới");
        inApp.setBody("Mã " + command.voucherCode() + " cho ưu đãi "
                + command.voucherName() + ". Nhập mã này tại checkout để sử dụng.");
        inApp.setCategory(Category.MARKETING.name());
        inApp.setDeepLink(command.deepLink());
        inApp.setExpiresAt(command.expiresAt());
        inAppNotificationRepository.save(inApp);
        meterRegistry.counter("notification_requests_total",
                "eventType", "VOUCHER_GRANTED", "category", Category.MARKETING.name())
                .increment();
        return new AcceptedNotification(request.getPublicId(), request.getStatus().name(),
                false, 2);
    }

    @Transactional(readOnly = true)
    public RequestDetails get(String publicId) {
        NotificationRequest request = requestRepository.findByPublicId(publicId)
                .orElseThrow(() -> notFound("Notification request was not found"));
        List<DeliveryDetails> deliveries = deliveryRepository
                .findByNotificationRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .map(delivery -> toDetails(delivery, request))
                .toList();
        RecipientSummary recipient = recipientSummary(request.getId());
        return new RequestDetails(request.getPublicId(), request.getIdempotencyKey(),
                request.getSourceService(), request.getSourceEventId(), request.getEventType(),
                request.getCorrelationId(), request.getTemplateKey(), request.getTemplateCommitSha(),
                request.getTemplateVersion(), request.getLocale(), request.getCategory().name(),
                request.getPriority().name(), request.getStatus().name(), request.isTest(),
                recipient, request.getExpiresAt(), request.getCreatedAt(), request.getUpdatedAt(),
                deliveries);
    }

    @Transactional
    public RequestDetails cancel(String publicId) {
        NotificationRequest request = requestRepository.findByPublicId(publicId)
                .orElseThrow(() -> notFound("Notification request was not found"));
        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new NotificationException("NOTIFICATION_ALREADY_COMPLETED",
                    "A completed notification cannot be cancelled", HttpStatus.CONFLICT);
        }
        request.setStatus(RequestStatus.CANCELLED);
        deliveryRepository.findByNotificationRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .filter(delivery -> delivery.getStatus() == DeliveryStatus.PENDING
                        || delivery.getStatus() == DeliveryStatus.RETRY_SCHEDULED)
                .forEach(delivery -> delivery.setStatus(DeliveryStatus.CANCELLED));
        return get(publicId);
    }

    @Transactional
    public DeliveryDetails retryDelivery(String deliveryPublicId) {
        NotificationDelivery delivery = deliveryRepository.findByPublicId(deliveryPublicId)
                .orElseThrow(() -> notFound("Notification delivery was not found"));
        NotificationRequest request = requestRepository.findById(delivery.getNotificationRequestId())
                .orElseThrow(() -> notFound("Notification request was not found"));
        RetryDecision decision = retryDecision(delivery, request);
        if (!decision.allowed()) {
            throw new NotificationException(decision.code(), decision.reason(), HttpStatus.CONFLICT);
        }
        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextRetryAt(Instant.now());
        delivery.setFailureCategory(null);
        delivery.setFailureCode(null);
        delivery.setFailureMessage(null);
        deadLetterRepository.findByNotificationDeliveryId(delivery.getId()).ifPresent(deadLetter -> {
            deadLetter.setReprocessCount(deadLetter.getReprocessCount() + 1);
            deadLetter.setReprocessedAt(Instant.now());
            deadLetterRepository.save(deadLetter);
        });
        return toDetails(delivery, request);
    }

    @Transactional(readOnly = true)
    public AttentionDetails attentionDetails(NotificationDeadLetter deadLetter) {
        NotificationDelivery delivery = deliveryRepository.findById(deadLetter.getNotificationDeliveryId())
                .orElse(null);
        NotificationRequest request = delivery == null ? null
                : requestRepository.findById(delivery.getNotificationRequestId()).orElse(null);
        RecipientSummary recipient = request == null ? null : recipientSummary(request.getId());
        RetryDecision decision = delivery == null || request == null
                ? new RetryDecision(false, "DELIVERY_NOT_FOUND",
                "Không còn tìm thấy lượt gửi gốc để xử lý lại.")
                : retryDecision(delivery, request);
        return new AttentionDetails(
                deadLetter.getId(), deadLetter.getNotificationDeliveryId(),
                delivery == null ? null : delivery.getPublicId(),
                request == null ? null : request.getPublicId(),
                request == null ? null : request.getEventType(),
                request == null ? null : request.getTemplateKey(),
                request == null ? null : request.getSourceService(),
                request == null ? null : request.getExpiresAt(),
                delivery == null || delivery.getChannel() == null ? null : delivery.getChannel().name(),
                delivery == null || delivery.getStatus() == null ? null : delivery.getStatus().name(),
                recipient, deadLetter.getReason(),
                delivery == null || delivery.getFailureCategory() == null
                        ? null : delivery.getFailureCategory().name(),
                delivery == null ? 0 : delivery.getAttemptCount(),
                deadLetter.getReprocessCount(), deadLetter.getCreatedAt(),
                decision.allowed(), decision.allowed() ? null : decision.reason());
    }

    private boolean isMarketingSuppressed(CreateNotificationCommand command, Channel channel) {
        if (command.category() != Category.MARKETING || command.recipient().userPublicId() == null) {
            return false;
        }
        return preferenceRepository.findByUserPublicIdAndChannelAndCategory(
                        command.recipient().userPublicId(), channel, Category.MARKETING.name())
                .map(preference -> !preference.isEnabled())
                .orElse(false);
    }

    private void validateRecipient(CreateNotificationCommand command) {
        for (Channel channel : command.channels()) {
            boolean missing = switch (channel) {
                case EMAIL -> command.recipient().email() == null || command.recipient().email().isBlank();
                case SMS -> command.recipient().phone() == null || command.recipient().phone().isBlank();
                case IN_APP -> command.recipient().userPublicId() == null
                        || command.recipient().userPublicId().isBlank();
                case WEB_PUSH -> command.recipient().webPushSubscription() == null
                        || command.recipient().webPushSubscription().isBlank();
            };
            if (missing) {
                throw new NotificationException("RECIPIENT_REQUIRED",
                        "Recipient destination is required for " + channel, HttpStatus.BAD_REQUEST);
            }
        }
        if (command.expiresAt() != null && !command.expiresAt().isAfter(Instant.now())) {
            throw new NotificationException("NOTIFICATION_EXPIRED",
                    "Notification expiry must be in the future", HttpStatus.BAD_REQUEST);
        }
    }

    private String providerName(Channel channel) {
        return switch (channel) {
            case EMAIL -> "smtp";
            case IN_APP -> "in-app";
            case WEB_PUSH -> "mock-web-push";
            case SMS -> "mock-sms";
        };
    }

    private DeliveryDetails toDetails(NotificationDelivery delivery, NotificationRequest request) {
        List<AttemptDetails> attempts = delivery.getId() == null ? List.of() : attemptRepository
                .findByNotificationDeliveryIdOrderByAttemptNumberAsc(delivery.getId()).stream()
                .map(this::toAttemptDetails)
                .toList();
        RetryDecision decision = retryDecision(delivery, request);
        return new DeliveryDetails(delivery.getPublicId(), delivery.getChannel().name(),
                delivery.getProvider(), delivery.getStatus().name(), delivery.getProviderMessageId(),
                delivery.getTemplateCommitSha(), delivery.getTemplateVersion(),
                delivery.getTemplateCommitSha() != null,
                delivery.getFailureCategory() == null ? null : delivery.getFailureCategory().name(),
                delivery.getFailureCode(), delivery.getFailureMessage(), delivery.getAttemptCount(),
                delivery.getNextRetryAt(), delivery.getSentAt(), delivery.getDeliveredAt(),
                decision.allowed(), decision.allowed() ? null : decision.reason(), attempts);
    }

    private RetryDecision retryDecision(
            NotificationDelivery delivery, NotificationRequest request) {
        if (delivery.getStatus() != DeliveryStatus.FAILED
                && delivery.getStatus() != DeliveryStatus.DEAD_LETTERED) {
            return new RetryDecision(false, "DELIVERY_NOT_RETRYABLE",
                    "Lượt gửi này không ở trạng thái có thể thử lại.");
        }
        if (request.getExpiresAt() != null && !request.getExpiresAt().isAfter(Instant.now())) {
            String reason = isOtp(request)
                    ? "Không thể gửi lại vì mã OTP đã hết hạn. Người dùng cần yêu cầu một mã mới."
                    : "Không thể gửi lại vì thông báo này đã hết hiệu lực. Hãy tạo một yêu cầu mới sau khi kiểm tra nghiệp vụ.";
            return new RetryDecision(false, "NOTIFICATION_EXPIRED_RETRY_FORBIDDEN", reason);
        }
        if (delivery.getFailureCategory() != null
                && delivery.getFailureCategory() != FailureCategory.TRANSIENT
                && delivery.getFailureCategory() != FailureCategory.RATE_LIMITED
                && delivery.getFailureCategory() != FailureCategory.AUTHENTICATION_ERROR) {
            return new RetryDecision(false, "DELIVERY_FAILURE_NOT_RETRYABLE",
                    "Cần sửa người nhận, nội dung mẫu hoặc dữ liệu đầu vào trước khi tạo yêu cầu mới.");
        }
        return new RetryDecision(true, null, null);
    }

    private boolean isOtp(NotificationRequest request) {
        return String.valueOf(request.getEventType()).toUpperCase().contains("OTP")
                || String.valueOf(request.getTemplateKey()).toUpperCase().contains("OTP");
    }

    @Transactional(readOnly = true)
    public RecipientSummary recipientSummary(Long requestId) {
        return recipientRepository.findFirstByNotificationRequestId(requestId)
                .map(recipient -> new RecipientSummary(
                        recipient.getUserPublicId(),
                        maskEmail(cryptoService.decrypt(recipient.getEmailEncrypted())),
                        maskPhone(cryptoService.decrypt(recipient.getPhoneEncrypted())),
                        recipient.getRecipientType()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ChannelOutcome> channelOutcomes(Long requestId) {
        return deliveryRepository.findByNotificationRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(delivery -> new ChannelOutcome(
                        delivery.getChannel().name(),
                        delivery.getStatus().name()))
                .toList();
    }

    private String maskEmail(String value) {
        if (value == null || value.isBlank()) return null;
        int separator = value.indexOf('@');
        if (separator <= 0) return "***";
        return value.substring(0, 1) + "***" + value.substring(separator);
    }

    private String maskPhone(String value) {
        if (value == null || value.isBlank()) return null;
        int visible = Math.min(3, value.length());
        return "***" + value.substring(value.length() - visible);
    }

    private AttemptDetails toAttemptDetails(NotificationDeliveryAttempt attempt) {
        return new AttemptDetails(attempt.getAttemptNumber(), attempt.getProvider(), attempt.getOutcome(),
                attempt.getFailureCategory() == null ? null : attempt.getFailureCategory().name(),
                attempt.getFailureCode(), attempt.getDurationMs(), attempt.getCreatedAt());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new NotificationException("PAYLOAD_SERIALIZATION_FAILED",
                    "Notification payload is not valid JSON", HttpStatus.BAD_REQUEST);
        }
    }

    private NotificationException notFound(String message) {
        return new NotificationException("NOTIFICATION_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }

    public record RequestDetails(
            String publicId,
            String idempotencyKey,
            String sourceService,
            String sourceEventId,
            String eventType,
            String correlationId,
            String templateKey,
            String templateCommitSha,
            String templateVersion,
            String locale,
            String category,
            String priority,
            String status,
            boolean test,
            RecipientSummary recipient,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt,
            List<DeliveryDetails> deliveries) {
    }

    public record DeliveryDetails(
            String publicId,
            String channel,
            String provider,
            String status,
            String providerMessageId,
            String templateCommitSha,
            String templateVersion,
            boolean renderedSnapshotAvailable,
            String failureCategory,
            String failureCode,
            String failureMessage,
            int attemptCount,
            Instant nextRetryAt,
            Instant sentAt,
            Instant deliveredAt,
            boolean retryAllowed,
            String retryBlockedReason,
            List<AttemptDetails> attempts) {
    }

    public record RecipientSummary(
            String userPublicId,
            String maskedEmail,
            String maskedPhone,
            String recipientType) {
    }

    public record ChannelOutcome(String channel, String status) {
    }

    public record AttentionDetails(
            Long id,
            Long notificationDeliveryId,
            String deliveryPublicId,
            String requestPublicId,
            String eventType,
            String templateKey,
            String sourceService,
            Instant expiresAt,
            String channel,
            String status,
            RecipientSummary recipient,
            String reason,
            String failureCategory,
            int attemptCount,
            int reprocessCount,
            Instant createdAt,
            boolean retryAllowed,
            String retryBlockedReason) {
    }

    private record RetryDecision(boolean allowed, String code, String reason) {
    }

    public record AttemptDetails(
            int attemptNumber,
            String provider,
            String outcome,
            String failureCategory,
            String failureCode,
            long durationMs,
            Instant createdAt) {
    }
}
