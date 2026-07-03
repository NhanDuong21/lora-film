package com.project.paymentservice;

import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.PaymentOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired
    private BookingPaymentGuardRepository guardRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;
    
    @Test
    void testPessimisticGuardLocking() throws InterruptedException {
        // Prepare guard
        transactionTemplate.execute(status -> {
            guardRepository.insertIfAbsent(999L);
            return null;
        });
        
        CountDownLatch tx1Ready = new CountDownLatch(1);
        CountDownLatch tx1Done = new CountDownLatch(1);
        long[] times = new long[2]; // times[0] = tx1 end sleep, times[1] = tx2 acquire lock

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                transactionTemplate.execute(status -> {
                    guardRepository.findByBookingIdForUpdate(999L).orElseThrow();
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
                    guardRepository.findByBookingIdForUpdate(999L).orElseThrow();
                    times[1] = System.currentTimeMillis();
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        tx1Done.await();
        executor.shutdown();

        // Wait a bit for Tx2 to finish if it hasn't already
        Thread.sleep(500);

        assertTrue(times[1] >= times[0], "Transaction 2 should acquire lock only after Transaction 1 finishes sleeping and releases it");
    }
}
