package com.project.paymentservice;

import com.project.paymentservice.entity.*;
import com.project.paymentservice.enumtype.*;
import com.project.paymentservice.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class PersistenceAndConstraintTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingPaymentGuardRepository guardRepository;

    @Autowired
    private PaymentLogRepository logRepository;

    @Autowired
    private PaymentIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookRepository;

    @Autowired
    private PaymentOutboxEventRepository outboxRepository;

    @Test
    @Transactional
    void testSameBookingAllowsMultipleAttempts() {
        Payment p1 = Payment.builder()
                .paymentTransactionCode("TXN-1")
                .bookingId(1L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p1);
        
        Payment p2 = Payment.builder()
                .paymentTransactionCode("TXN-2")
                .bookingId(1L)
                .accountId(10L)
                .attemptNumber(2) // different attempt
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p2);
    }
    
    @Test
    @Transactional
    void testDuplicateBookingAttemptFails() {
        Payment p1 = Payment.builder()
                .paymentTransactionCode("TXN-DUP-1")
                .bookingId(99L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p1);
        
        Payment p3 = Payment.builder()
                .paymentTransactionCode("TXN-DUP-2")
                .bookingId(99L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        assertThrows(Exception.class, () -> paymentRepository.saveAndFlush(p3));
    }
    
    @Test
    @Transactional
    void testMultipleNullExternalTransactionIdAllowed() {
        Payment p4 = Payment.builder()
                .paymentTransactionCode("TXN-4")
                .bookingId(2L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .externalTransactionId(null)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p4);
        
        Payment p5 = Payment.builder()
                .paymentTransactionCode("TXN-5")
                .bookingId(3L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.MOMO)
                .externalTransactionId(null)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p5);
    }
    
    @Test
    @Transactional
    void testDeleteRestrictions() {
        Payment p1 = Payment.builder()
                .paymentTransactionCode("TXN-RESTRICT")
                .bookingId(100L)
                .accountId(10L)
                .attemptNumber(1)
                .amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.VNPAY)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentRepository.saveAndFlush(p1);
        
        PaymentLog log = PaymentLog.builder()
                .paymentId(p1.getId())
                .eventType(PaymentLogEventType.PAYMENT_INITIATED)
                .source("API")
                .actorType(ActorType.CUSTOMER)
                .currentStatus(PaymentStatus.PENDING)
                .build();
        logRepository.saveAndFlush(log);
        
        // Hard delete should fail because of ON DELETE RESTRICT
        assertThrows(Exception.class, () -> {
            paymentRepository.delete(p1);
            paymentRepository.flush();
        });
    }

    @Test
    @Transactional
    void testIdempotencyConstraints() {
        PaymentIdempotencyRecord r1 = PaymentIdempotencyRecord.builder()
                .accountId(1L)
                .operation("CREATE_PAYMENT")
                .idempotencyKey("KEY1")
                .requestHash("hash1")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        idempotencyRepository.saveAndFlush(r1);

        PaymentIdempotencyRecord r2 = PaymentIdempotencyRecord.builder()
                .accountId(1L)
                .operation("CREATE_PAYMENT")
                .idempotencyKey("KEY1")
                .requestHash("hash2")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        assertThrows(Exception.class, () -> idempotencyRepository.saveAndFlush(r2));
    }
}
