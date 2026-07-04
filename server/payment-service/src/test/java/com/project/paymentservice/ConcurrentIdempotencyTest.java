package com.project.paymentservice;

import com.project.paymentservice.client.booking.BookingPaymentClient;
import com.project.paymentservice.client.booking.BookingPaymentContext;
import com.project.paymentservice.dto.request.CreatePaymentRequest;
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
import java.time.LocalDateTime;
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
    void concurrentSameIdempotencyKeyShouldCreateExactlyOnePayment() throws InterruptedException {
        when(bookingClient.getPaymentContext(anyLong())).thenAnswer(invocation -> {
            BookingPaymentContext ctx = new BookingPaymentContext();
            ctx.setBookingId(invocation.getArgument(0));
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
            // Simulate slight delay to increase race window
            Thread.sleep(50);
            return ctx;
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
                    if ("IDEMPOTENCY_REQUEST_IN_PROGRESS".equals(e.getErrorCode())) {
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

        // Verify exactly one payment created
        assertEquals(1, paymentRepository.count(), "Exactly one payment should be created");
        assertEquals(1, idempotencyRepository.count(), "Exactly one idempotency record should exist");
        assertEquals(1, successCount.get(), "Exactly one request should succeed or replay");
        assertEquals(1, conflictCount.get(), "The loser request should return IDEMPOTENCY_REQUEST_IN_PROGRESS");
        
        executor.shutdown();
    }
}
