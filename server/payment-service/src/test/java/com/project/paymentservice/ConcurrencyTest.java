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
        guardRepository.insertIfAbsent(999L);
        
        CountDownLatch tx1Ready = new CountDownLatch(1);
        CountDownLatch tx1Done = new CountDownLatch(1);
        AtomicBoolean tx2AcquiredLockBeforeTx1Done = new AtomicBoolean(false);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        executor.submit(() -> {
            transactionTemplate.execute(status -> {
                BookingPaymentGuard guard = guardRepository.findByBookingIdForUpdate(999L).orElseThrow();
                tx1Ready.countDown();
                try {
                    // Hold lock for a while
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
            tx1Done.countDown();
        });
        
        executor.submit(() -> {
            try {
                tx1Ready.await(); // wait until tx1 has lock
                transactionTemplate.execute(status -> {
                    // This should block until tx1 finishes
                    guardRepository.findByBookingIdForUpdate(999L).orElseThrow();
                    if (tx1Done.getCount() > 0) {
                        // We acquired lock but tx1 is not done, this means lock failed!
                        tx2AcquiredLockBeforeTx1Done.set(true);
                    }
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        tx1Done.await();
        executor.shutdown();
        
        assertTrue(!tx2AcquiredLockBeforeTx1Done.get(), "Transaction 2 should not acquire lock before Transaction 1 completes");
    }
}
