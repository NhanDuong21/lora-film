package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
import com.project.paymentservice.entity.PaymentIdempotencyRecord;
import com.project.paymentservice.exception.BusinessException;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentIdempotencyTest {

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
    void concurrentSameIdempotencyKeyShouldCreateExactlyOnePayment() throws InterruptedException {
        when(bookingClient.getPaymentContext(anyLong())).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(invocation.getArgument(0));
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
            // Simulate slight delay to increase race window
            Thread.sleep(50);
            return TestFixtures.complete(ctx);
        });

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        CreatePaymentRequest req = new CreatePaymentRequest(1009L, "MOCK");

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    paymentService.createPayment(15L, "concurrent-key-1", req);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if ("PAYMENT_REQUEST_IN_PROGRESS".equals(e.getErrorCode())) {
                        conflictCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads at once
        doneLatch.await(10, TimeUnit.SECONDS);

        // Verify exactly one payment created
        assertEquals(1, paymentRepository.count(), "Exactly one payment should be created");
        assertEquals(1, idempotencyRepository.count(), "Exactly one idempotency record should exist");
        assertEquals(1, successCount.get(), "Exactly one request should succeed or replay");
        assertEquals(1, conflictCount.get(), "The loser request should return IDEMPOTENCY_REQUEST_IN_PROGRESS");

        executor.shutdown();
    }

    @Test
    void existingProcessingIdempotencyRecordShouldReturnInProgress() {
        // Setup existing processing record
        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey("existing-processing-key");
        record.setRequestHash(com.project.paymentservice.service.CanonicalHashUtil.hashCreatePayment(15L, 1009L, "MOCK"));
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.PROCESSING);
        record.setLockedBy("another-request-owner");
        record.setLockedAt(Instant.now());
        record.setLockedUntil(Instant.now().plusSeconds(60));
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        CreatePaymentRequest req = new CreatePaymentRequest(1009L, "MOCK");

        BusinessException exception = org.junit.jupiter.api.Assertions.assertThrows(
            BusinessException.class,
            () -> paymentService.createPayment(15L, "existing-processing-key", req)
        );

        assertEquals("PAYMENT_REQUEST_IN_PROGRESS", exception.getErrorCode());
        assertEquals(0, paymentRepository.count(), "No Payment created");
        assertEquals(0, guardRepository.count(), "No Guard created or changed");
        assertEquals(0, snapshotRepository.count(), "No analytics snapshot created");
        assertEquals(0, logRepository.count(), "No Payment log created");
        org.mockito.Mockito.verifyNoInteractions(bookingClient);
    }

    @Test
    void concurrentCancelWithSameKeyShouldApplyExactlyOnce() throws InterruptedException {
        com.project.paymentservice.entity.Payment p = new com.project.paymentservice.entity.Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("CAN-CONCURRENT");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(com.project.paymentservice.enumtype.PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(com.project.paymentservice.enumtype.PaymentStatus.PENDING);
        p.setExpiresAt(Instant.now().plusSeconds(900));
        p = paymentRepository.save(TestFixtures.complete(p));

        com.project.paymentservice.entity.BookingPaymentGuard guard = new com.project.paymentservice.entity.BookingPaymentGuard();
        guard.setBookingPublicId(p.getBookingPublicId());
        guard.setBookingId(1001L);
        guard.setActivePaymentId(p.getId());
        guard.setNextAttemptNumber(1);
        guardRepository.save(guard);

        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        final Long paymentId = p.getId();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    paymentService.cancelPayment(15L, "concurrent-cancel-key", paymentId);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if ("PAYMENT_REQUEST_IN_PROGRESS".equals(e.getErrorCode())) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads at once
        doneLatch.await(10, TimeUnit.SECONDS);

        // Verify exactly one cancel applied
        com.project.paymentservice.entity.Payment after = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.PaymentStatus.CANCELLED, after.getStatus());
        assertEquals(1, idempotencyRepository.count(), "Exactly one idempotency record should exist");
        assertEquals(1, successCount.get(), "Exactly one request should succeed or replay");
        assertEquals(1, conflictCount.get(), "The loser request should return IDEMPOTENCY_REQUEST_IN_PROGRESS");
        assertEquals(1, logRepository.count(), "Exactly one business cancellation log");

        com.project.paymentservice.entity.BookingPaymentGuard afterGuard = guardRepository.findByBookingId(1001L).orElseThrow();
        org.junit.jupiter.api.Assertions.assertNull(afterGuard.getActivePaymentId());

        executor.shutdown();
    }

    @Test
    void expiredProcessingLeaseShouldBeRecovered() {
        String key = "concurrent-loser-test";
        CreatePaymentRequest req = new CreatePaymentRequest(1011L, "MOCK");
        PaymentIdempotencyRecord record = new PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey(key);
        record.setRequestHash(
                com.project.paymentservice.service.CanonicalHashUtil.hashCreatePayment(
                        15L, 1011L, "MOCK"));
        record.setProcessingStatus(
                com.project.paymentservice.enumtype.IdempotencyProcessingStatus.PROCESSING);
        record.setLockedBy("crashed-worker");
        record.setLockedAt(Instant.now().minusSeconds(120));
        record.setLockedUntil(Instant.now().minusSeconds(60));
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        when(bookingClient.getPaymentContext(1011L)).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setAccountId(15L);
            ctx.setBookingId(1011L);
            ctx.setAmount(new BigDecimal("150000.0"));
            ctx.setCurrency("VND");
            return TestFixtures.complete(ctx);
        });

        paymentService.createPayment(15L, key, req);

        PaymentIdempotencyRecord recovered = idempotencyRepository.findById(record.getId())
                .orElseThrow();
        assertEquals(
                com.project.paymentservice.enumtype.IdempotencyProcessingStatus.COMPLETED,
                recovered.getProcessingStatus());
        assertEquals(1, paymentRepository.count(), "Exactly one payment should be created");
        assertEquals(2, logRepository.count());
    }
}
