package com.project.notificationservice.service.impl;

import com.project.notificationservice.dto.request.SendNotificationRequest;
import com.project.notificationservice.dto.response.SendNotificationResponse;
import com.project.notificationservice.entity.NotificationLog;
import com.project.notificationservice.entity.NotificationTemplate;
import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.exception.BusinessException;
import com.project.notificationservice.provider.NotificationSender;
import com.project.notificationservice.provider.NotificationSenderResolver;
import com.project.notificationservice.provider.model.ProviderSendRequest;
import com.project.notificationservice.provider.model.ProviderSendResult;
import com.project.notificationservice.provider.util.LogMaskingUtils;
import com.project.notificationservice.repository.NotificationLogRepository;
import com.project.notificationservice.repository.NotificationTemplateRepository;
import com.project.notificationservice.service.NotificationService;
import com.project.notificationservice.service.TemplateRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateRenderer templateRenderer;
    private final NotificationSenderResolver senderResolver;
    private final TransactionTemplate transactionTemplate;

    public NotificationServiceImpl(NotificationTemplateRepository templateRepository,
                                   NotificationLogRepository logRepository,
                                   TemplateRenderer templateRenderer,
                                   NotificationSenderResolver senderResolver,
                                   TransactionTemplate transactionTemplate) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.templateRenderer = templateRenderer;
        this.senderResolver = senderResolver;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        // Phase 1: Request Validation
        validateRequest(request);

        String trimmedRecipient = request.getRecipient() != null ? request.getRecipient().trim() : null;

        // Phase 2: Idempotency Verification
        if (request.getEventId() != null && !request.getEventId().trim().isEmpty()) {
            Optional<NotificationLog> existing = logRepository.findByEventId(request.getEventId().trim());
            if (existing.isPresent()) {
                return buildIdempotentResponse(existing.get());
            }
        }
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
            Optional<NotificationLog> existing = logRepository.findByIdempotencyKey(request.getIdempotencyKey().trim());
            if (existing.isPresent()) {
                return buildIdempotentResponse(existing.get());
            }
        }

        // Phase 3: Template Loading & Rendering
        String actualTitle;
        String actualContent;
        String templateCode = null;

        if (request.getTemplateCode() != null && !request.getTemplateCode().trim().isEmpty()) {
            templateCode = request.getTemplateCode().trim().toUpperCase();
            NotificationTemplate template = templateRepository.findByTemplateCode(templateCode)
                    .orElseThrow(() -> new BusinessException(
                            "Notification template not found",
                            "NOTIFICATION_TEMPLATE_NOT_FOUND",
                            HttpStatus.NOT_FOUND
                    ));

            if (Boolean.FALSE.equals(template.getIsActive())) {
                throw new BusinessException(
                        "Notification template is disabled",
                        "NOTIFICATION_TEMPLATE_DISABLED",
                        HttpStatus.CONFLICT
                );
            }

            if (template.getChannelType() != request.getChannelType()) {
                Map<String, Object> errorData = Map.of(
                        "templateChannel", template.getChannelType().name(),
                        "requestedChannel", request.getChannelType().name()
                );
                throw new BusinessException(
                        "Requested channel does not match template channel",
                        "NOTIFICATION_TEMPLATE_CHANNEL_MISMATCH",
                        HttpStatus.BAD_REQUEST,
                        errorData
                );
            }

            actualTitle = templateRenderer.render(template.getTitle(), request.getVariables());
            actualContent = templateRenderer.render(template.getContent(), request.getVariables());
        } else {
            actualTitle = request.getTitle();
            actualContent = request.getContent();
        }

        // Phase 4: Create PENDING Log & Commit Transaction
        NotificationLog logEntry = new NotificationLog();
        logEntry.setTemplateCode(templateCode);
        logEntry.setEventId(request.getEventId() != null && !request.getEventId().trim().isEmpty() ? request.getEventId().trim() : null);
        logEntry.setIdempotencyKey(request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty() ? request.getIdempotencyKey().trim() : null);
        logEntry.setUserId(request.getUserId());
        logEntry.setRecipient(trimmedRecipient);
        logEntry.setChannelType(request.getChannelType());
        logEntry.setRequestSource(request.getRequestSource().trim());

        if (request.getReference() != null) {
            logEntry.setReferenceType(request.getReference().getType() != null && !request.getReference().getType().trim().isEmpty() ? request.getReference().getType().trim() : null);
            logEntry.setReferenceId(request.getReference().getId() != null && !request.getReference().getId().trim().isEmpty() ? request.getReference().getId().trim() : null);
        }

        logEntry.setActualTitle(actualTitle);
        logEntry.setActualContent(actualContent);
        logEntry.setStatus("PENDING");

        NotificationLog savedLog;
        try {
            savedLog = transactionTemplate.execute(status -> logRepository.saveAndFlush(logEntry));
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate request detected on concurrent DB insert for eventId: {} or idempotencyKey: {}", request.getEventId(), request.getIdempotencyKey());
            // Attempt to retrieve existing duplicate log
            NotificationLog existing = null;
            if (request.getEventId() != null && !request.getEventId().trim().isEmpty()) {
                existing = logRepository.findByEventId(request.getEventId().trim()).orElse(null);
            }
            if (existing == null && request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty()) {
                existing = logRepository.findByIdempotencyKey(request.getIdempotencyKey().trim()).orElse(null);
            }
            if (existing != null) {
                return buildIdempotentResponse(existing);
            }
            throw ex;
        }

        if (savedLog == null) {
            throw new BusinessException(
                    "Failed to save notification log",
                    "INTERNAL_SERVER_ERROR",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        // Phase 5: Resolve Provider and Invoke Outside Transaction context
        NotificationSender provider = senderResolver.resolve(request.getChannelType());
        if (provider == null) {
            updateLogStatus(savedLog.getId(), "FAILED", "PROVIDER_UNAVAILABLE", "No provider resolved for channel type: " + request.getChannelType(), null);
            throw new BusinessException(
                    "Notification provider is unavailable",
                    "NOTIFICATION_PROVIDER_UNAVAILABLE",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        ProviderSendRequest sendRequest = ProviderSendRequest.builder()
                .notificationId(String.valueOf(savedLog.getId()))
                .userId(request.getUserId())
                .channelType(request.getChannelType())
                .recipient(trimmedRecipient)
                .title(actualTitle)
                .content(actualContent)
                .templateCode(templateCode)
                .build();

        ProviderSendResult sendResult;
        try {
            sendResult = provider.send(sendRequest);
        } catch (Exception e) {
            log.error("Exception thrown during provider notification delivery", e);
            updateLogStatus(savedLog.getId(), "FAILED", "PROVIDER_UNAVAILABLE", "Provider delivery exception: " + e.getMessage(), null);
            throw new BusinessException(
                    "Notification sending failed due to provider error",
                    "NOTIFICATION_SEND_FAILED",
                    HttpStatus.BAD_GATEWAY
            );
        }

        // Phase 6: Update Log Status & Return
        if (sendResult.isSuccess()) {
            LocalDateTime sentAt = LocalDateTime.now();
            updateLogStatus(savedLog.getId(), "SENT", null, null, sentAt, sendResult.getProviderName(), sendResult.getProviderMessageId());
            
            SendNotificationResponse response = new SendNotificationResponse();
            response.setNotificationId(savedLog.getId());
            response.setEventId(savedLog.getEventId());
            response.setTemplateCode(savedLog.getTemplateCode());
            response.setUserId(savedLog.getUserId());
            response.setChannelType(savedLog.getChannelType());
            response.setRecipient(maskRecipientResponse(savedLog.getChannelType(), trimmedRecipient));
            response.setStatus("SENT");
            response.setSentAt(sentAt);
            return response;
        } else {
            updateLogStatus(savedLog.getId(), "FAILED", sendResult.getFailureCode(), sendResult.getErrorMessage(), null, sendResult.getProviderName(), sendResult.getProviderMessageId());
            throw new BusinessException(
                    "Notification sending failed: " + sendResult.getErrorMessage(),
                    "NOTIFICATION_SEND_FAILED",
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private void validateRequest(SendNotificationRequest request) {
        if (request.getRequestSource() == null || request.getRequestSource().trim().isEmpty()) {
            throw new BusinessException("requestSource is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new BusinessException("userId must be greater than 0", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (request.getChannelType() == null) {
            throw new BusinessException("channelType is required", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        // Channel Support limits
        if (request.getChannelType() == NotificationChannel.SMS || request.getChannelType() == NotificationChannel.PUSH_NOTIFICATION) {
            throw new BusinessException("Channel type not supported", "NOTIFICATION_CHANNEL_NOT_SUPPORTED", HttpStatus.CONFLICT);
        }

        // Event / Idempotency Key validation
        boolean hasEventId = request.getEventId() != null && !request.getEventId().trim().isEmpty();
        boolean hasIdempotency = request.getIdempotencyKey() != null && !request.getIdempotencyKey().trim().isEmpty();
        if (!hasEventId && !hasIdempotency) {
            throw new BusinessException("Either eventId or idempotencyKey must be provided", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        // Mutual Exclusion validation
        boolean hasTemplate = request.getTemplateCode() != null && !request.getTemplateCode().trim().isEmpty();
        boolean hasFreeForm = (request.getTitle() != null && !request.getTitle().trim().isEmpty())
                || (request.getContent() != null && !request.getContent().trim().isEmpty());

        if (hasTemplate && hasFreeForm) {
            throw new BusinessException("Cannot specify both templateCode and title/content", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (!hasTemplate && !hasFreeForm) {
            throw new BusinessException("Must specify either templateCode or title/content", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        // Free-Form parameter requirements
        if (!hasTemplate) {
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()
                    || request.getContent() == null || request.getContent().trim().isEmpty()) {
                throw new BusinessException("Title and content are required in free-form mode", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
            if (request.getVariables() != null && !request.getVariables().isEmpty()) {
                throw new BusinessException("Variables are not allowed in free-form mode", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
        }

        // Reference validation
        if (request.getReference() != null) {
            boolean hasType = request.getReference().getType() != null && !request.getReference().getType().trim().isEmpty();
            boolean hasId = request.getReference().getId() != null && !request.getReference().getId().trim().isEmpty();
            if (hasType != hasId) {
                throw new BusinessException("Reference type and id must both be present or both be null", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
        }

        // Channel Recipient validation
        String trimmedRecipient = request.getRecipient() != null ? request.getRecipient().trim() : null;
        if (request.getChannelType() == NotificationChannel.EMAIL) {
            if (trimmedRecipient == null || trimmedRecipient.isEmpty()) {
                throw new BusinessException("Recipient is required for EMAIL channel", "NOTIFICATION_INVALID_RECIPIENT", HttpStatus.BAD_REQUEST);
            }
            if (!EMAIL_PATTERN.matcher(trimmedRecipient).matches()) {
                throw new BusinessException("Invalid email format", "NOTIFICATION_INVALID_RECIPIENT", HttpStatus.BAD_REQUEST);
            }
            // SMTP Header Injection prevention: check recipient and title (if freeform) for newlines
            if (trimmedRecipient.contains("\n") || trimmedRecipient.contains("\r")) {
                throw new BusinessException("Recipient must not contain newline characters", "NOTIFICATION_INVALID_RECIPIENT", HttpStatus.BAD_REQUEST);
            }
            if (!hasTemplate && request.getTitle() != null && (request.getTitle().contains("\n") || request.getTitle().contains("\r"))) {
                throw new BusinessException("Email title must not contain newline characters", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void updateLogStatus(Long logId, String status, String failureCode, String errorMessage, LocalDateTime sentAt) {
        updateLogStatus(logId, status, failureCode, errorMessage, sentAt, null, null);
    }

    private void updateLogStatus(Long logId, String status, String failureCode, String errorMessage, LocalDateTime sentAt, String providerName, String providerMessageId) {
        try {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                NotificationLog logEntry = logRepository.findById(logId).orElse(null);
                if (logEntry != null) {
                    logEntry.setStatus(status);
                    logEntry.setFailureCode(failureCode);
                    logEntry.setErrorMessage(errorMessage);
                    logEntry.setSentAt(sentAt);
                    if (providerName != null) {
                        logEntry.setProviderName(providerName);
                    }
                    if (providerMessageId != null) {
                        logEntry.setProviderMessageId(providerMessageId);
                    }
                    logRepository.saveAndFlush(logEntry);
                }
            });
        } catch (Exception e) {
            log.error("Failed to update notification log status for logId: {}", logId, e);
        }
    }

    private SendNotificationResponse buildIdempotentResponse(NotificationLog logEntry) {
        SendNotificationResponse response = new SendNotificationResponse();
        response.setNotificationId(logEntry.getId());
        response.setEventId(logEntry.getEventId());
        response.setTemplateCode(logEntry.getTemplateCode());
        response.setUserId(logEntry.getUserId());
        response.setChannelType(logEntry.getChannelType());
        response.setRecipient(maskRecipientResponse(logEntry.getChannelType(), logEntry.getRecipient()));
        response.setStatus(logEntry.getStatus());
        response.setSentAt(logEntry.getSentAt());
        response.setIdempotent(true);
        return response;
    }

    private String maskRecipientResponse(NotificationChannel channel, String recipient) {
        if (channel == NotificationChannel.EMAIL) {
            return LogMaskingUtils.maskRecipient(recipient);
        }
        return recipient;
    }
}
