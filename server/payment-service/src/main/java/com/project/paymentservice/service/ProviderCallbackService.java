package com.project.paymentservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentWebhookEvent;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.WebhookProcessingStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentWebhookEventRepository;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service
public class ProviderCallbackService {
    private static final Logger log = LoggerFactory.getLogger(ProviderCallbackService.class);
    private final PaymentProviderRegistry registry;
    private final PaymentWebhookEventRepository webhookRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentTransactionService transactionService;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Value("${payment.providers.vnpay.trust-return-url:false}")
    private boolean trustVnPayReturnUrl;

    public ProviderCallbackService(
            PaymentProviderRegistry registry,
            PaymentWebhookEventRepository webhookRepository,
            PaymentRepository paymentRepository,
            PaymentTransactionService transactionService,
            ObjectMapper objectMapper) {
        this.registry = registry;
        this.webhookRepository = webhookRepository;
        this.paymentRepository = paymentRepository;
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public CallbackOutcome process(
            ProviderCode provider, Map<String, String> parameters, String rawBody) {
        PaymentProvider adapter = registry.getProvider(provider);
        ProviderCallbackResult result = adapter.verifyCallback(parameters, rawBody);
        String hashSource = rawBody == null || rawBody.isBlank()
                ? writeJson(new java.util.TreeMap<>(parameters)) : rawBody;
        String rawHash = sha256(hashSource);

        PaymentWebhookEvent existing = webhookRepository
                .findByProviderCodeAndDeduplicationKey(provider, result.getDeduplicationKey())
                .orElse(null);
        if (existing != null) {
            if (!rawHash.equals(existing.getRawBodyHash())) {
                existing.setLastErrorSanitized(
                        "CONFLICTING_PAYLOAD_HASH:" + rawHash);
                webhookRepository.save(existing);
                transactionService.recordWebhookPayloadConflict(
                        existing.getPaymentId(),
                        existing.getId(),
                        result.getDeduplicationKey() + ":" + rawHash,
                        "Provider reused a callback key with a different payload hash");
                throw new BusinessException("PROVIDER_EVENT_CONFLICT",
                        "Callback provider trùng khóa nhưng khác payload", HttpStatus.CONFLICT);
            }
            return new CallbackOutcome(existing.getPaymentId(), true,
                    Boolean.TRUE.equals(existing.getSignatureValid()),
                    existing.getProcessingStatus() == WebhookProcessingStatus.PROCESSED);
        }

        Payment payment = result.getProviderOrderId() == null ? null
                : paymentRepository.findByProviderCodeAndProviderOrderId(
                        provider, result.getProviderOrderId()).orElse(null);
        PaymentWebhookEvent event = saveInbox(provider, result, rawHash, payment);
        if (!result.isSignatureValid()) {
            markFailed(event.getId(), "INVALID_PROVIDER_SIGNATURE");
            return new CallbackOutcome(
                    payment == null ? null : payment.getId(), false, false, false);
        }
        if (payment == null) {
            markFailed(event.getId(), "PAYMENT_ORDER_NOT_FOUND");
            return new CallbackOutcome(null, false, true, false);
        }
        try {
            transactionService.applyProviderResult(provider, result, event.getId());
            markProcessed(event.getId());
            return new CallbackOutcome(payment.getId(), false, true, true);
        } catch (BusinessException exception) {
            if ("PAYMENT_AMOUNT_MISMATCH".equals(exception.getErrorCode())) {
                markProcessed(event.getId());
            } else {
                markFailed(event.getId(), exception.getErrorCode());
            }
            throw exception;
        }
    }

    public ReturnOutcome verifyReturn(
            ProviderCode provider, Map<String, String> parameters) {
        PaymentProvider adapter = registry.getProvider(provider);
        ProviderCallbackResult result = adapter.verifyReturn(parameters);
        if (!result.isSignatureValid()) {
            return new ReturnOutcome(false, null, null);
        }
        Payment payment = paymentRepository.findByProviderCodeAndProviderOrderId(
                provider, result.getProviderOrderId()).orElse(null);
        if (payment != null && payment.getStatus() == PaymentStatus.PROCESSING) {
            resolveVerifiedReturn(provider, adapter, payment, result);
        }
        return payment == null
                ? new ReturnOutcome(true, null, null)
                : new ReturnOutcome(true, payment.getPublicId(), payment.getBookingPublicId());
    }

    private void resolveVerifiedReturn(
            ProviderCode provider, PaymentProvider adapter, Payment payment, ProviderCallbackResult result) {
        if (trustVnPayReturnUrl && provider == ProviderCode.VNPAY && result != null && ("SUCCESS".equals(result.getResult()) || "CANCELLED".equals(result.getResult()))) {
            log.info("Directly trusting verified Return URL redirect result: provider={}, paymentId={}, result={}",
                    provider, payment.getId(), result.getResult());
            transactionService.applyProviderResult(provider, result, null);
            return;
        }

        try {
            // Browser Return remains navigation data only. It merely prompts a
            // server-to-server provider query; only that authoritative query
            // result is allowed to mutate Payment and Booking.
            var authoritativeResult = adapter.queryStatus(payment);
            if (authoritativeResult.isPresent()) {
                transactionService.applyProviderResult(
                        provider, authoritativeResult.get(), null);
                return;
            }
        } catch (RuntimeException exception) {
            log.warn("Authoritative provider query failed after verified Return: "
                            + "provider={}, paymentId={}, error={}",
                    provider, payment.getId(), exception.getClass().getSimpleName());
        }

        // Second fallback: if queryStatus failed/returned empty, but we trust return URL fallback
        if (trustVnPayReturnUrl && result != null && ("SUCCESS".equals(result.getResult()) || "CANCELLED".equals(result.getResult()))) {
            log.info("Authoritative query returned empty. Falling back to verified redirect callback result: provider={}, paymentId={}, result={}",
                    provider, payment.getId(), result.getResult());
            transactionService.applyProviderResult(provider, result, null);
            return;
        }

        int retryDelaySeconds = Math.max(5, adapter.recoveryRetryDelaySeconds());
        transactionService.scheduleProviderStatusCheck(
                payment.getId(), Instant.now().plusSeconds(retryDelaySeconds));
    }

    @Transactional
    protected PaymentWebhookEvent saveInbox(
            ProviderCode provider,
            ProviderCallbackResult result,
            String rawHash,
            Payment payment) {
        PaymentWebhookEvent event = new PaymentWebhookEvent();
        event.setProviderCode(provider);
        event.setProviderEventId(result.getExternalTransactionId());
        event.setDeduplicationKey(result.getDeduplicationKey());
        event.setPaymentId(payment == null ? null : payment.getId());
        event.setPaymentTransactionCode(
                payment == null ? null : payment.getPaymentTransactionCode());
        event.setProviderOrderId(result.getProviderOrderId());
        event.setExternalTransactionId(result.getExternalTransactionId());
        event.setEventType(result.getEventType());
        event.setRawBodyHash(rawHash);
        event.setSanitizedPayload(
                result.getSanitizedPayload() == null ? "{}" : result.getSanitizedPayload());
        event.setSignatureValid(result.isSignatureValid());
        event.setSignatureAlgorithm(provider == ProviderCode.VNPAY
                ? "HMACSHA512" : "HMACSHA256");
        event.setProcessingStatus(WebhookProcessingStatus.PENDING);
        return webhookRepository.saveAndFlush(event);
    }

    @Transactional
    protected void markProcessed(Long id) {
        PaymentWebhookEvent event = webhookRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalStateException("Webhook event missing"));
        event.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
        event.setProcessedAt(java.time.Instant.now());
        event.setLockedBy(null);
        event.setLockedAt(null);
        event.setLockedUntil(null);
        webhookRepository.save(event);
    }

    @Transactional
    protected void markFailed(Long id, String error) {
        PaymentWebhookEvent event = webhookRepository.findAndLockById(id)
                .orElseThrow(() -> new IllegalStateException("Webhook event missing"));
        event.setProcessingStatus(WebhookProcessingStatus.FAILED);
        event.setLastErrorSanitized(error);
        webhookRepository.save(event);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public record CallbackOutcome(
            Long paymentId,
            boolean idempotent,
            boolean signatureValid,
            boolean processed) {
    }

    public record ReturnOutcome(
            boolean signatureValid,
            String paymentPublicId,
            String bookingPublicId) {
    }
}
