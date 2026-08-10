package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.provider.PaymentProvider;
import com.project.paymentservice.provider.PaymentProviderRegistry;
import com.project.paymentservice.provider.PaymentSession;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.project.paymentservice.repository.PaymentLogRepository;
import com.project.paymentservice.repository.PaymentRepository;
import com.project.paymentservice.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import com.project.paymentservice.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class AtomicRollbackTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private BookingPaymentGuardRepository guardRepository;

    @Autowired
    private PaymentAnalyticsSnapshotRepository snapshotRepository;

    @SpyBean
    private PaymentLogRepository logRepository;

    @MockBean
    private BookingPaymentClient bookingClient;

    @MockBean
    private PaymentProviderRegistry providerRegistry;

    @MockBean
    private PaymentProvider paymentProvider;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void tearDown() {
        databaseCleaner.clean();
    }

    @Test
    void paymentCreationShouldRollbackWhenLogPersistenceFails() {
        when(bookingClient.getPaymentContext(2001L)).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(2001L);
            ctx.setAccountId(15L);
            ctx.setAmount(new BigDecimal("100"));
            ctx.setCurrency("VND");
            ctx.setPayable(true);
            ctx.setExpiresAt(java.time.Instant.now().plusSeconds(900));
            BookingPaymentContext.AnalyticsSnapshotData snap = new BookingPaymentContext.AnalyticsSnapshotData();
            snap.setMovieId(1L);
            snap.setMovieTitle("Movie");
            snap.setTicketCount(1);
            ctx.setAnalyticsSnapshot(snap);
            return TestFixtures.complete(ctx);
        });

        // Force log persistence to fail
        doThrow(new DataIntegrityViolationException("Simulated log failure"))
                .when(logRepository).save(any());

        CreatePaymentRequest req = new CreatePaymentRequest(2001L, "MOCK");

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            paymentService.createPayment(15L, "idem-rollback-1", req);
        });
        assertEquals("INTERNAL_SERVER_ERROR", ex.getErrorCode());

        assertEquals(0, paymentRepository.count(), "No Payment row committed");
        assertEquals(0, guardRepository.count(), "No Guard created or changed");
        assertEquals(0, snapshotRepository.count(), "No analytics snapshot committed");
        assertEquals(0, logRepository.count(), "No partial Payment Log");

        assertEquals(1, idempotencyRepository.count(), "Create idempotency record exists");
        PaymentIdempotencyRecord createIdemp = idempotencyRepository.findAll().get(0);
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED, createIdemp.getProcessingStatus(), "Create idempotency should be marked FAILED");
    }

    @Test
    void cancelFailureShouldMarkIdempotencyRecordAsFailed() {
        Payment payment = new Payment();
        payment.setAccountId(15L);
        payment.setBookingId(2002L);
        payment.setPaymentTransactionCode("ROLLBACK-CANCEL-2002");
        payment.setAmount(new BigDecimal("100"));
        payment.setPaymentMethod(com.project.paymentservice.enumtype.PaymentMethod.MOCK);
        payment.setAttemptNumber(1);
        payment.setStatus(com.project.paymentservice.enumtype.PaymentStatus.PENDING);
        payment.setExpiresAt(Instant.now().plusSeconds(900));
        payment = paymentRepository.saveAndFlush(TestFixtures.complete(payment));
        guardRepository.saveAndFlush(TestFixtures.guard(payment));
        Long paymentId = payment.getId();

        doThrow(new DataIntegrityViolationException("Simulated log failure during cancel"))
                .when(logRepository).save(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            paymentService.cancelPayment(15L, "idem-cancel-1", paymentId);
        });
        assertEquals("INTERNAL_SERVER_ERROR", ex.getErrorCode());

        Payment afterRollback = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.PaymentStatus.PENDING,
                afterRollback.getStatus(), "Payment cancellation must roll back atomically");

        assertEquals(1, idempotencyRepository.count(), "Cancel idempotency record exists");
        PaymentIdempotencyRecord cancelIdemp = idempotencyRepository.findAll().stream()
                .filter(r -> "CANCEL_PAYMENT".equals(r.getOperation()))
                .findFirst().orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED, cancelIdemp.getProcessingStatus(), "Cancel idempotency should be marked FAILED");

        assertEquals(0, logRepository.count(), "No partial cancellation log");
    }
}
