package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionBoundaryTest {

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

    @Autowired
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
    void bookingPaymentContextCallShouldRunOutsideTransaction() {
        boolean[] isTransactionActive = new boolean[1];

        when(bookingClient.getPaymentContext(1005L)).thenAnswer(invocation -> {
            isTransactionActive[0] = TransactionSynchronizationManager.isActualTransactionActive();
            
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(1005L);
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
            return ctx;
        });

        when(providerRegistry.getProvider(any())).thenReturn(paymentProvider);
        when(paymentProvider.supportedMethod()).thenReturn(com.project.paymentservice.enumtype.PaymentMethod.MOCK);
        when(paymentProvider.createSession(any())).thenReturn(
                new PaymentSession("ORDER", "SESSION", "URL", LocalDateTime.now().plusMinutes(15)));

        CreatePaymentRequest req = new CreatePaymentRequest(1005L, "MOCK");
        paymentService.createPayment(15L, "idem-boundary-1", req);

        assertFalse(isTransactionActive[0], "Booking API call must execute OUTSIDE a DB transaction");
    }

    @Test
    void providerSessionCreationShouldRunOutsideTransaction() {
        boolean[] isTransactionActive = new boolean[1];

        when(bookingClient.getPaymentContext(1006L)).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(1006L);
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
            return ctx;
        });

        when(providerRegistry.getProvider(any())).thenReturn(paymentProvider);
        when(paymentProvider.supportedMethod()).thenReturn(com.project.paymentservice.enumtype.PaymentMethod.MOCK);
        
        when(paymentProvider.createSession(any())).thenAnswer(invocation -> {
            isTransactionActive[0] = TransactionSynchronizationManager.isActualTransactionActive();
            return new PaymentSession("ORDER", "SESSION", "URL", LocalDateTime.now().plusMinutes(15));
        });

        CreatePaymentRequest req = new CreatePaymentRequest(1006L, "MOCK");
        paymentService.createPayment(15L, "idem-boundary-2", req);

        assertFalse(isTransactionActive[0], "Provider session creation must execute OUTSIDE a DB transaction");
    }
}
