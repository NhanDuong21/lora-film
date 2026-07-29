package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentResultResponse;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.OutboxDestination;
import com.project.paymentservice.enumtype.OutboxStatus;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.ReconciliationStatus;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.ProviderCallbackResult;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import com.project.paymentservice.repository.PaymentReconciliationCaseRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import com.project.paymentservice.repository.PaymentWebhookEventRepository;
import com.project.paymentservice.service.PaymentOutboxWorker;
import com.project.paymentservice.service.OutboxDeliveryStateService;
import com.project.paymentservice.service.AdminPaymentService;
import com.project.paymentservice.service.ProviderCallbackService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentReleaseOneWorkflowTest {

    @Autowired
    private ProviderCallbackService callbackService;
    @Autowired
    private PaymentOutboxWorker outboxWorker;
    @Autowired
    private OutboxDeliveryStateService outboxStateService;
    @Autowired
    private AdminPaymentService adminPaymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private BookingPaymentGuardRepository guardRepository;
    @Autowired
    private PaymentWebhookEventRepository webhookRepository;
    @Autowired
    private PaymentOutboxEventRepository outboxRepository;
    @Autowired
    private PaymentReconciliationCaseRepository reconciliationRepository;
    @Autowired
    private PaymentRefundRepository refundRepository;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private PaymentProviderRegistry providerRegistry;
    @MockBean
    private BookingPaymentClient bookingClient;
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentProvider provider;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        reset(providerRegistry, bookingClient, kafkaTemplate);
        provider = mock(PaymentProvider.class);
        when(providerRegistry.getProvider(ProviderCode.VNPAY)).thenReturn(provider);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    void duplicateCallbackIsIdempotentAndRevenueIsPublishedOnlyAfterBookingAccepts() {
        Payment payment = persistProcessingPayment(Instant.now().plusSeconds(600));
        ProviderCallbackResult result = successResult(payment, "callback-1");
        when(provider.verifyCallback(anyMap(), anyString())).thenReturn(result);

        ProviderCallbackService.CallbackOutcome first = callbackService.process(
                ProviderCode.VNPAY, Map.of("event", "callback-1"), "same-raw-body");
        ProviderCallbackService.CallbackOutcome duplicate = callbackService.process(
                ProviderCode.VNPAY, Map.of("event", "callback-1"), "same-raw-body");

        assertTrue(first.processed());
        assertTrue(duplicate.idempotent());
        assertEquals(1, webhookRepository.count());
        assertEquals(1, outboxRepository.count());
        assertEquals(PaymentStatus.SUCCESS,
                paymentRepository.findById(payment.getId()).orElseThrow().getStatus());

        BookingPaymentResultResponse accepted = new BookingPaymentResultResponse();
        accepted.setAccepted(true);
        accepted.setReconciliationRequired(false);
        when(bookingClient.notifyPaymentResult(anyString(), any())).thenReturn(accepted);

        outboxWorker.deliver();

        assertEquals(1, outboxRepository.findAll().stream()
                .filter(event -> event.getDestination() == OutboxDestination.ANALYTICS_KAFKA)
                .filter(event -> event.getStatus() == OutboxStatus.PENDING)
                .count());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());

        outboxWorker.deliver();

        assertEquals(2, outboxRepository.findAll().stream()
                .filter(event -> event.getStatus() == OutboxStatus.PUBLISHED)
                .count());
        verify(kafkaTemplate).send(
                anyString(),
                org.mockito.ArgumentMatchers.eq(payment.getPublicId()),
                anyString());
    }

    @Test
    void changedPayloadForSameProviderKeyCreatesReconciliation() {
        Payment payment = persistProcessingPayment(Instant.now().plusSeconds(600));
        ProviderCallbackResult result = successResult(payment, "callback-conflict");
        when(provider.verifyCallback(anyMap(), anyString())).thenReturn(result);

        callbackService.process(
                ProviderCode.VNPAY, Map.of("event", "callback-conflict"), "original-body");

        BusinessException conflict = assertThrows(BusinessException.class, () ->
                callbackService.process(
                        ProviderCode.VNPAY,
                        Map.of("event", "callback-conflict"),
                        "changed-body"));

        assertEquals("PROVIDER_EVENT_CONFLICT", conflict.getErrorCode());
        assertEquals(1, webhookRepository.count());
        assertEquals(1, reconciliationRepository.count());
        assertEquals(ReconciliationStatus.REQUIRED,
                paymentRepository.findById(payment.getId()).orElseThrow()
                        .getReconciliationStatus());
    }

    @Test
    void bookingConflictCompletesDeliveryCreatesReconciliationAndSuppressesRevenue() {
        Payment payment = persistProcessingPayment(Instant.now().plusSeconds(600));
        when(provider.verifyCallback(anyMap(), anyString()))
                .thenReturn(successResult(payment, "callback-booking-conflict"));
        callbackService.process(
                ProviderCode.VNPAY,
                Map.of("event", "callback-booking-conflict"),
                "booking-conflict-body");

        when(bookingClient.notifyPaymentResult(anyString(), any()))
                .thenThrow(new BusinessException(
                        "BOOKING_RECONCILIATION_REQUIRED",
                        "Booking cần đối soát",
                        HttpStatus.CONFLICT));

        outboxWorker.deliver();

        assertEquals(1, reconciliationRepository.count());
        assertEquals(1, outboxRepository.findAll().stream()
                .filter(event -> event.getDestination() == OutboxDestination.BOOKING_SERVICE_REST)
                .filter(event -> event.getStatus() == OutboxStatus.PUBLISHED)
                .count());
        assertFalse(outboxRepository.findAll().stream()
                .anyMatch(event -> event.getDestination() == OutboxDestination.ANALYTICS_KAFKA));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        assertEquals(1, refundRepository.count());
        assertEquals("BOOKING_CONFIRMATION_FAILED",
                refundRepository.findAll().getFirst().getReasonCode());
    }

    @Test
    void lateProviderSuccessRecordsFinancialTruthButRequiresReconciliation() {
        Payment payment = persistProcessingPayment(Instant.now().minusSeconds(1));
        when(provider.verifyCallback(anyMap(), anyString()))
                .thenReturn(successResult(payment, "callback-late"));

        callbackService.process(
                ProviderCode.VNPAY, Map.of("event", "callback-late"), "late-body");

        Payment stored = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, stored.getStatus());
        assertEquals(ReconciliationStatus.REQUIRED, stored.getReconciliationStatus());
        assertEquals("LATE_PROVIDER_SUCCESS", stored.getReconciliationReason());
        assertEquals(1, reconciliationRepository.count());
        assertEquals(1, outboxRepository.count());
        assertEquals(1, refundRepository.count());
        assertEquals("LATE_PROVIDER_SUCCESS",
                refundRepository.findAll().getFirst().getReasonCode());
    }

    @Test
    void secondFinancialSuccessForSameBookingCreatesAutomaticFullRefund() {
        Payment first = persistProcessingPayment(Instant.now().plusSeconds(600));
        when(provider.verifyCallback(anyMap(), anyString()))
                .thenReturn(successResult(first, "callback-first-success"));
        callbackService.process(
                ProviderCode.VNPAY,
                Map.of("event", "callback-first-success"),
                "first-success-body");

        Payment duplicate = persistAdditionalProcessingPayment(first);
        when(provider.verifyCallback(anyMap(), anyString()))
                .thenReturn(successResult(duplicate, "callback-duplicate-capture"));
        callbackService.process(
                ProviderCode.VNPAY,
                Map.of("event", "callback-duplicate-capture"),
                "duplicate-capture-body");

        Payment stored = paymentRepository.findById(duplicate.getId()).orElseThrow();
        assertEquals(PaymentStatus.SUCCESS, stored.getStatus());
        assertEquals(ReconciliationStatus.REQUIRED, stored.getReconciliationStatus());
        assertEquals("DUPLICATE_FINANCIAL_SUCCESS", stored.getReconciliationReason());
        assertEquals(1, refundRepository.count());
        assertTrue(refundRepository.findAll().getFirst().isAutomatic());
        assertEquals("DUPLICATE_CAPTURE",
                refundRepository.findAll().getFirst().getReasonCode());
        assertEquals(0, duplicate.getAmount().compareTo(
                refundRepository.findAll().getFirst().getRequestedAmount()));
    }

    @Test
    void invalidProviderSignatureIsAuditedWithoutChangingPayment() {
        Payment payment = persistProcessingPayment(Instant.now().plusSeconds(600));
        ProviderCallbackResult result = successResult(payment, "callback-invalid-signature");
        result.setSignatureValid(false);
        when(provider.verifyCallback(anyMap(), anyString())).thenReturn(result);

        ProviderCallbackService.CallbackOutcome outcome = callbackService.process(
                ProviderCode.VNPAY,
                Map.of("event", "callback-invalid-signature"),
                "invalid-signature-body");

        assertFalse(outcome.signatureValid());
        assertFalse(outcome.processed());
        assertEquals(PaymentStatus.PROCESSING,
                paymentRepository.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(1, webhookRepository.count());
        assertEquals("FAILED", webhookRepository.findAll().getFirst()
                .getProcessingStatus().name());
        assertEquals(0, outboxRepository.count());
        Long webhookId = webhookRepository.findAll().getFirst().getId();
        BusinessException replayRejected = assertThrows(
                BusinessException.class,
                () -> adminPaymentService.replayWebhook(webhookId));
        assertEquals("WEBHOOK_SIGNATURE_INVALID", replayRejected.getErrorCode());
    }

    @Test
    void kafkaOutageRetriesDeadLettersAndReplaysSameEvent() {
        PaymentOutboxEvent event = new PaymentOutboxEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setAggregateType("PAYMENT");
        event.setAggregateId(UUID.randomUUID().toString());
        event.setEventType("PAYMENT_SUCCEEDED");
        event.setSchemaVersion("1.0");
        event.setDestination(OutboxDestination.ANALYTICS_KAFKA);
        event.setPayload("{\"eventId\":\"analytics-outage\"}");
        event.setStatus(OutboxStatus.PENDING);
        event.setAttemptCount(0);
        event = outboxRepository.saveAndFlush(event);
        String originalEventId = event.getEventId();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new IllegalStateException("Kafka unavailable")));

        for (int attempt = 1; attempt <= 8; attempt++) {
            outboxWorker.deliver();
            event = outboxRepository.findById(event.getId()).orElseThrow();
            if (attempt < 8) {
                assertEquals(OutboxStatus.FAILED, event.getStatus());
                event.setNextRetryAt(Instant.now().minusSeconds(1));
                outboxRepository.saveAndFlush(event);
            }
        }

        assertEquals(OutboxStatus.DEAD_LETTER, event.getStatus());
        assertEquals(8, event.getAttemptCount());

        outboxStateService.replay(originalEventId);
        event = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());
        assertEquals(originalEventId, event.getEventId());

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        outboxWorker.deliver();

        event = outboxRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertEquals(originalEventId, event.getEventId());
    }

    private Payment persistProcessingPayment(Instant deadline) {
        Payment payment = new Payment();
        payment.setPublicId(UUID.randomUUID().toString());
        payment.setPaymentTransactionCode("PAY-" + UUID.randomUUID());
        payment.setBookingPublicId(UUID.randomUUID().toString());
        payment.setBookingId(901L);
        payment.setAccountId(15L);
        payment.setAttemptNumber(1);
        payment.setAmount(new BigDecimal("325000"));
        payment.setCurrency("VND");
        payment.setBookingAmountLockedAt(Instant.now().minusSeconds(30));
        payment.setBookingExpiresAt(deadline);
        payment.setPaymentMethod(PaymentMethod.ONLINE);
        payment.setProviderCode(ProviderCode.VNPAY);
        payment.setProviderOrderId("ORDER-" + UUID.randomUUID());
        payment.setProviderSessionId("SESSION-" + UUID.randomUUID());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setReconciliationStatus(ReconciliationStatus.NONE);
        payment = paymentRepository.saveAndFlush(payment);

        guardRepository.saveAndFlush(TestFixtures.guard(payment));

        jdbcTemplate.update("""
                INSERT INTO payment_analytics_snapshots (
                    payment_id, movie_id, movie_public_id, movie_title,
                    showtime_public_id, cinema_public_id, ticket_count,
                    ticket_amount, food_amount, discount_amount, total_amount, currency
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                payment.getId(),
                9L,
                UUID.randomUUID().toString(),
                "Nhà Có Năm Nàng Tiên",
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                2,
                new BigDecimal("170000"),
                new BigDecimal("155000"),
                BigDecimal.ZERO,
                new BigDecimal("325000"),
                "VND");
        return payment;
    }

    private Payment persistAdditionalProcessingPayment(Payment successfulPayment) {
        Payment payment = new Payment();
        payment.setPublicId(UUID.randomUUID().toString());
        payment.setPaymentTransactionCode("PAY-" + UUID.randomUUID());
        payment.setBookingPublicId(successfulPayment.getBookingPublicId());
        payment.setBookingId(successfulPayment.getBookingId());
        payment.setAccountId(successfulPayment.getAccountId());
        payment.setAttemptNumber(2);
        payment.setAmount(successfulPayment.getAmount());
        payment.setCurrency(successfulPayment.getCurrency());
        payment.setBookingAmountLockedAt(successfulPayment.getBookingAmountLockedAt());
        payment.setBookingExpiresAt(Instant.now().plusSeconds(600));
        payment.setPaymentMethod(PaymentMethod.ONLINE);
        payment.setProviderCode(ProviderCode.VNPAY);
        payment.setProviderOrderId("ORDER-" + UUID.randomUUID());
        payment.setProviderSessionId("SESSION-" + UUID.randomUUID());
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setReconciliationStatus(ReconciliationStatus.NONE);
        payment = paymentRepository.saveAndFlush(payment);

        var guard = guardRepository.findById(successfulPayment.getBookingPublicId())
                .orElseThrow();
        guard.setActivePaymentId(payment.getId());
        guard.setNextAttemptNumber(3);
        guardRepository.saveAndFlush(guard);
        return payment;
    }

    private ProviderCallbackResult successResult(Payment payment, String deduplicationKey) {
        ProviderCallbackResult result = new ProviderCallbackResult();
        result.setSignatureValid(true);
        result.setDeduplicationKey(deduplicationKey);
        result.setProviderOrderId(payment.getProviderOrderId());
        result.setExternalTransactionId("TX-" + deduplicationKey);
        result.setResult("SUCCESS");
        result.setResponseCode("00");
        result.setAmount(payment.getAmount());
        result.setCurrency(payment.getCurrency());
        result.setEventType("PAYMENT_RESULT");
        result.setOccurredAt(Instant.now());
        result.setSanitizedPayload("{\"result\":\"SUCCESS\"}");
        return result;
    }
}
