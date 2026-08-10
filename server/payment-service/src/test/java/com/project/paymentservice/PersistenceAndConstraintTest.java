package com.project.paymentservice;

import com.project.paymentservice.entity.*;
import com.project.paymentservice.enumtype.*;
import com.project.paymentservice.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void legacyPaymentSuccessLogCanBeHydratedForAdminDetail() {
        Payment payment = new Payment();
        payment.setPaymentTransactionCode("TXN-LEGACY-SUCCESS");
        payment.setBookingId(501L);
        payment.setAccountId(10L);
        payment.setAttemptNumber(1);
        payment.setAmount(new BigDecimal("100000"));
        payment.setPaymentMethod(PaymentMethod.CASH);
        payment.setExpiresAt(Instant.now().plusSeconds(900));
        Payment savedPayment = paymentRepository.saveAndFlush(TestFixtures.complete(payment));

        jdbcTemplate.update("""
                INSERT INTO payment_logs
                    (payment_id, event_type, source, actor_type, current_status)
                VALUES (?, 'PAYMENT_SUCCESS', 'LEGACY_IMPORT', 'SYSTEM', 'SUCCESS')
                """, savedPayment.getId());
        entityManager.clear();

        List<PaymentLog> logs = logRepository.findByPaymentIdOrderByCreatedAtAsc(savedPayment.getId());

        assertEquals(1, logs.size());
        assertEquals(PaymentLogEventType.PAYMENT_SUCCESS, logs.getFirst().getEventType());
    }

    @Test
    @Transactional
    void testSameBookingAllowsMultipleAttempts() {
        Payment p1 = new Payment();
        p1.setPaymentTransactionCode("TXN-1");
        p1.setBookingId(1L);
        p1.setAccountId(10L);
        p1.setAttemptNumber(1);
        p1.setAmount(new BigDecimal("100000"));
        p1.setPaymentMethod(PaymentMethod.VNPAY);
        p1.setExpiresAt(Instant.now().plusSeconds(900));
        p1 = paymentRepository.saveAndFlush(TestFixtures.complete(p1));
        
        Payment p2 = new Payment();
        p2.setPaymentTransactionCode("TXN-2");
        p2.setBookingId(1L);
        p2.setAccountId(10L);
        p2.setAttemptNumber(2); // different attempt
        p2.setAmount(new BigDecimal("100000"));
        p2.setPaymentMethod(PaymentMethod.VNPAY);
        p2.setExpiresAt(Instant.now().plusSeconds(900));
        paymentRepository.saveAndFlush(TestFixtures.complete(p2));
    }
    
    @Test
    @Transactional
    void testDuplicateBookingAttemptFails() {
        Payment p1 = new Payment();
        p1.setPaymentTransactionCode("TXN-DUP-1");
        p1.setBookingId(99L);
        p1.setAccountId(10L);
        p1.setAttemptNumber(1);
        p1.setAmount(new BigDecimal("100000"));
        p1.setPaymentMethod(PaymentMethod.VNPAY);
        p1.setExpiresAt(Instant.now().plusSeconds(900));
        p1 = paymentRepository.saveAndFlush(TestFixtures.complete(p1));
        
        Payment p3 = new Payment();
        p3.setPaymentTransactionCode("TXN-DUP-2");
        p3.setBookingId(99L);
        p3.setAccountId(10L);
        p3.setAttemptNumber(1);
        p3.setAmount(new BigDecimal("100000"));
        p3.setPaymentMethod(PaymentMethod.VNPAY);
        p3.setExpiresAt(Instant.now().plusSeconds(900));
        assertThrows(Exception.class, () -> paymentRepository.saveAndFlush(TestFixtures.complete(p3)));
    }
    
    @Test
    @Transactional
    void testMultipleNullExternalTransactionIdAllowed() {
        Payment p4 = new Payment();
        p4.setPaymentTransactionCode("TXN-4");
        p4.setBookingId(2L);
        p4.setAccountId(10L);
        p4.setAttemptNumber(1);
        p4.setAmount(new BigDecimal("100000"));
        p4.setPaymentMethod(PaymentMethod.MOMO);
        p4.setExternalTransactionId(null);
        p4.setExpiresAt(Instant.now().plusSeconds(900));
        paymentRepository.saveAndFlush(TestFixtures.complete(p4));
        
        Payment p5 = new Payment();
        p5.setPaymentTransactionCode("TXN-5");
        p5.setBookingId(3L);
        p5.setAccountId(10L);
        p5.setAttemptNumber(1);
        p5.setAmount(new BigDecimal("100000"));
        p5.setPaymentMethod(PaymentMethod.MOMO);
        p5.setExternalTransactionId(null);
        p5.setExpiresAt(Instant.now().plusSeconds(900));
        paymentRepository.saveAndFlush(TestFixtures.complete(p5));
    }
    
    @Test
    @Transactional
    void testDeleteRestrictions() {
        Payment p1 = new Payment();
        p1.setPaymentTransactionCode("TXN-RESTRICT");
        p1.setBookingId(100L);
        p1.setAccountId(10L);
        p1.setAttemptNumber(1);
        p1.setAmount(new BigDecimal("100000"));
        p1.setPaymentMethod(PaymentMethod.VNPAY);
        p1.setExpiresAt(Instant.now().plusSeconds(900));
        Payment savedPayment = paymentRepository.saveAndFlush(TestFixtures.complete(p1));
        
        PaymentLog log = new PaymentLog();
        log.setPaymentId(savedPayment.getId());
        log.setEventType(PaymentLogEventType.PAYMENT_INITIATED);
        log.setSource("API");
        log.setActorType(ActorType.CUSTOMER);
        log.setCurrentStatus(PaymentStatus.PENDING);
        logRepository.saveAndFlush(log);
        
        // Hard delete should fail because of ON DELETE RESTRICT
        assertThrows(Exception.class, () -> {
            paymentRepository.delete(savedPayment);
            paymentRepository.flush();
        });
    }

    @Test
    @Transactional
    void testIdempotencyConstraints() {
        PaymentIdempotencyRecord r1 = new PaymentIdempotencyRecord();
        r1.setAccountId(1L);
        r1.setOperation("CREATE_PAYMENT");
        r1.setIdempotencyKey("KEY1");
        r1.setRequestHash("hash1");
        r1.setExpiresAt(Instant.now().plusSeconds(86400));
        idempotencyRepository.saveAndFlush(r1);

        PaymentIdempotencyRecord r2 = new PaymentIdempotencyRecord();
        r2.setAccountId(1L);
        r2.setOperation("CREATE_PAYMENT");
        r2.setIdempotencyKey("KEY1");
        r2.setRequestHash("hash2");
        r2.setExpiresAt(Instant.now().plusSeconds(86400));
        assertThrows(Exception.class, () -> idempotencyRepository.saveAndFlush(r2));
    }
}
