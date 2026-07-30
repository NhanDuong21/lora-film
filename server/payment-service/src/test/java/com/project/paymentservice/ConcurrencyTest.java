package com.project.paymentservice;

import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentOutboxEvent;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired
    private BookingPaymentGuardRepository guardRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOutboxEventRepository outboxRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void pessimisticGuardLockShouldBlockSecondTransaction() throws InterruptedException {
        long uniqueId = System.currentTimeMillis() + 2000;
        String bookingPublicId = UUID.randomUUID().toString();
        transactionTemplate.execute(status -> {
            guardRepository.insertIfAbsent(bookingPublicId, uniqueId);
            return null;
        });
        
        CountDownLatch tx1Ready = new CountDownLatch(1);
        CountDownLatch tx1Done = new CountDownLatch(1);
        long[] times = new long[2]; // times[0] = tx1 end sleep, times[1] = tx2 acquire lock

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    guardRepository.findByBookingIdForUpdate(uniqueId).orElseThrow();
                    tx1Ready.countDown();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    times[0] = System.currentTimeMillis();
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                tx1Done.countDown();
            }
        });

        executor.submit(() -> {
            try {
                tx1Ready.await();
                transactionTemplate.execute(status -> {
                    guardRepository.findByBookingIdForUpdate(uniqueId).orElseThrow();
                    times[1] = System.currentTimeMillis();
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        tx1Done.await();
        executor.shutdown();

        Thread.sleep(500);

        Assertions.assertTrue(times[1] >= times[0], "Transaction 2 should acquire lock only after Transaction 1 finishes sleeping and releases it");
    }

    @Test
    void optimisticLockOnPaymentShouldRejectStaleUpdate() {
        long uniqueBookingId = System.currentTimeMillis() + 3000;
        Payment p = transactionTemplate.execute(status -> {
            Payment newP = new Payment();
            newP.setPaymentTransactionCode("TXN-OPT-" + System.currentTimeMillis());
            newP.setBookingId(uniqueBookingId);
            newP.setAccountId(1L);
            newP.setAttemptNumber(1);
            newP.setAmount(new BigDecimal("1000"));
            newP.setPaymentMethod(PaymentMethod.VNPAY);
            newP.setExpiresAt(Instant.now().plusSeconds(900));
            return paymentRepository.saveAndFlush(TestFixtures.complete(newP));
        });

        Payment pTx1 = paymentRepository.findById(p.getId()).orElseThrow();
        Payment pTx2 = paymentRepository.findById(p.getId()).orElseThrow();

        pTx1.setStatus(PaymentStatus.SUCCESS);
        pTx1.setSucceededAt(Instant.now());
        paymentRepository.saveAndFlush(pTx1);

        pTx2.setStatus(PaymentStatus.FAILED);
        pTx2.setFailedAt(Instant.now());
        Assertions.assertThrows(
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            () -> paymentRepository.saveAndFlush(pTx2)
        );
    }

    @Test
    void optimisticLockOnBookingGuardShouldRejectStaleUpdate() {
        long uniqueId = System.currentTimeMillis();
        BookingPaymentGuard guard = transactionTemplate.execute(status -> {
            BookingPaymentGuard newG = new BookingPaymentGuard();
            newG.setBookingPublicId(UUID.randomUUID().toString());
            newG.setBookingId(uniqueId);
            return guardRepository.saveAndFlush(newG);
        });

        BookingPaymentGuard g1 = guardRepository.findById(guard.getBookingPublicId()).orElseThrow();
        BookingPaymentGuard g2 = guardRepository.findById(guard.getBookingPublicId()).orElseThrow();

        g1.setNextAttemptNumber(2);
        guardRepository.saveAndFlush(g1);

        g2.setNextAttemptNumber(3);
        Assertions.assertThrows(
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            () -> guardRepository.saveAndFlush(g2)
        );
    }

    @Test
    void concurrentInsertIfAbsentShouldCreateExactlyOneGuard() throws InterruptedException {
        long uniqueId = System.currentTimeMillis() + 1000;
        String bookingPublicId = UUID.randomUUID().toString();
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    transactionTemplate.execute(status -> {
                        guardRepository.insertIfAbsent(bookingPublicId, uniqueId);
                        return null;
                    });
                } catch (Exception e) {} finally {
                    doneLatch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();
        
        long count = guardRepository.findAll().stream().filter(g -> g.getBookingId().equals(uniqueId)).count();
        Assertions.assertEquals(1L, count);
    }

    @Test
    void outboxClaimShouldSkipRowsLockedByAnotherWorker() throws InterruptedException {
        transactionTemplate.execute(status -> {
            for (int i=1; i<=5; i++) {
                PaymentOutboxEvent event = new PaymentOutboxEvent();
                event.setEventId("EVT-" + System.currentTimeMillis() + "-" + i);
                event.setAggregateType("PAYMENT");
                event.setAggregateId("AG-" + i);
                event.setEventType("CREATED");
                event.setSchemaVersion("1.0");
                event.setDestination(com.project.paymentservice.enumtype.OutboxDestination.ANALYTICS_KAFKA);
                event.setPayload("{}");
                outboxRepository.save(event);
            }
            return null;
        });

        CountDownLatch tx1Ready = new CountDownLatch(1);
        CountDownLatch tx1Done = new CountDownLatch(1);
        List<PaymentOutboxEvent> list1 = new ArrayList<>();
        List<PaymentOutboxEvent> list2 = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    Instant now = Instant.now();
                    list1.addAll(outboxRepository.findAndClaimPendingEvents(
                            now, now.plusSeconds(30), "worker-1", 3));
                    tx1Ready.countDown();
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                    return null;
                });
            } catch (Exception e) {} finally {
                tx1Done.countDown();
            }
        });

        executor.submit(() -> {
            try {
                tx1Ready.await();
                transactionTemplate.execute(status -> {
                    Instant now = Instant.now();
                    list2.addAll(outboxRepository.findAndClaimPendingEvents(
                            now, now.plusSeconds(30), "worker-2", 3));
                    return null;
                });
            } catch (Exception e) {}
        });

        tx1Done.await();
        executor.shutdown();
        Thread.sleep(500);

        Assertions.assertTrue(list1.size() > 0);
        
        for (PaymentOutboxEvent e1 : list1) {
            for (PaymentOutboxEvent e2 : list2) {
                Assertions.assertNotEquals(e1.getId(), e2.getId());
            }
        }
    }

    @Test
    void expiredOutboxLeaseShouldBeRecoveredByAnotherWorker() {
        PaymentOutboxEvent event = transactionTemplate.execute(status -> {
            PaymentOutboxEvent item = new PaymentOutboxEvent();
            item.setEventId("EVT-RECOVERY-" + System.currentTimeMillis());
            item.setAggregateType("PAYMENT");
            item.setAggregateId("AG-RECOVERY");
            item.setEventType("PAYMENT_RESULT");
            item.setSchemaVersion("1.0");
            item.setDestination(
                    com.project.paymentservice.enumtype.OutboxDestination.BOOKING_SERVICE_REST);
            item.setPayload("{}");
            item.setStatus(com.project.paymentservice.enumtype.OutboxStatus.PROCESSING);
            item.setLockedBy("worker-before-restart");
            item.setLockedAt(Instant.now().minusSeconds(60));
            item.setLockedUntil(Instant.now().minusSeconds(1));
            return outboxRepository.saveAndFlush(item);
        });

        List<PaymentOutboxEvent> recovered = transactionTemplate.execute(status ->
                outboxRepository.findAndClaimPendingEvents(
                        Instant.now(),
                        Instant.now().plusSeconds(30),
                        "worker-after-restart",
                        5));

        Assertions.assertNotNull(recovered);
        PaymentOutboxEvent reclaimed = recovered.stream()
                .filter(item -> event.getId().equals(item.getId()))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("worker-after-restart", reclaimed.getLockedBy());
    }
}
