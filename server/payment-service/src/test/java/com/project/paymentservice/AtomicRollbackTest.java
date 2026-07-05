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
import java.time.LocalDateTime;

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

    @BeforeEach
    void setUp() {
        snapshotRepository.deleteAllInBatch();
        logRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
    }

    @AfterEach
    void tearDown() {
        snapshotRepository.deleteAllInBatch();
        logRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        idempotencyRepository.deleteAllInBatch();
        guardRepository.deleteAllInBatch();
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
            ctx.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            BookingPaymentContext.AnalyticsSnapshotData snap = new BookingPaymentContext.AnalyticsSnapshotData();
            snap.setMovieId(1L);
            snap.setMovieTitle("Movie");
            snap.setTicketCount(1);
            ctx.setAnalyticsSnapshot(snap);
            return ctx;
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
        // We will test transition failure by simulating log failure during transition.
        when(bookingClient.getPaymentContext(2002L)).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(2002L);
            ctx.setAccountId(15L);
            ctx.setAmount(new BigDecimal("100"));
            ctx.setCurrency("VND");
            ctx.setPayable(true);
            ctx.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            BookingPaymentContext.AnalyticsSnapshotData snap = new BookingPaymentContext.AnalyticsSnapshotData();
            snap.setMovieId(1L);
            snap.setMovieTitle("Movie");
            snap.setTicketCount(1);
            ctx.setAnalyticsSnapshot(snap);
            return ctx;
        });

        when(providerRegistry.getProvider(any())).thenReturn(paymentProvider);
        when(paymentProvider.supportedMethod()).thenReturn(com.project.paymentservice.enumtype.PaymentMethod.MOCK);
        when(paymentProvider.createSession(any())).thenReturn(
                new PaymentSession("ORDER", "SESSION", "URL", LocalDateTime.now().plusMinutes(15)));

        CreatePaymentRequest req = new CreatePaymentRequest(2002L, "MOCK");
        paymentService.createPayment(15L, "idem-rollback-2", req);

        assertEquals(1, paymentRepository.count());
        Payment payment = paymentRepository.findAll().get(0);
        assertEquals(com.project.paymentservice.enumtype.PaymentStatus.PENDING, payment.getStatus());

        // Now we force log persistence to fail for the NEXT log insertion
        doThrow(new DataIntegrityViolationException("Simulated log failure during cancel"))
                .when(logRepository).save(any());

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            paymentService.cancelPayment(15L, "idem-cancel-1", payment.getId());
        });
        assertEquals("INTERNAL_SERVER_ERROR", ex.getErrorCode());

        // The state should remain PROCESSING
        Payment afterRollback = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.PaymentStatus.PENDING, afterRollback.getStatus(), "Original Payment status remains");

        assertEquals(2, idempotencyRepository.count(), "Idempotency for cancel exists (2 total)");
        PaymentIdempotencyRecord cancelIdemp = idempotencyRepository.findAll().stream()
                .filter(r -> "CANCEL_PAYMENT".equals(r.getOperation()))
                .findFirst().orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED, cancelIdemp.getProcessingStatus(), "Cancel idempotency should be marked FAILED");

        // The log count should be 2 (PAYMENT_CREATED and PROVIDER_SESSION_CREATED from the create flow)
        assertEquals(2, logRepository.count(), "No partial terminal log");
    }
}
