package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentResultResponse;
import com.project.paymentservice.entity.Payment;
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
import com.project.paymentservice.repository.PaymentWebhookEventRepository;
import com.project.paymentservice.service.PaymentOutboxWorker;
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
