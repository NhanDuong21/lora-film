package com.project.paymentservice.repository;

import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    @Test
    public void testPaymentPersistenceSuccess() {
        Payment payment = new Payment();
        payment.setPaymentTransactionCode("PAY-12345");
        payment.setBookingId(100L);
        payment.setAmount(new BigDecimal("150.00"));
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        Payment saved = paymentRepository.saveAndFlush(payment);
        
        assertNotNull(saved.getId());
        assertEquals("PAY-12345", saved.getPaymentTransactionCode());
        assertEquals(new BigDecimal("150.00"), saved.getAmount());
        assertEquals(0, saved.getVersion());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    public void testMultipleAttemptsAllowed() {
        Payment attempt1 = new Payment();
        attempt1.setPaymentTransactionCode("PAY-001");
        attempt1.setBookingId(200L);
        attempt1.setAmount(new BigDecimal("100.00"));
        attempt1.setPaymentMethod(PaymentMethod.MOMO);
        attempt1.setStatus(PaymentStatus.FAILED);
        attempt1.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        paymentRepository.saveAndFlush(attempt1);

        Payment attempt2 = new Payment();
        attempt2.setPaymentTransactionCode("PAY-002");
        attempt2.setBookingId(200L); // Same bookingId allowed
        attempt2.setAmount(new BigDecimal("100.00"));
        attempt2.setPaymentMethod(PaymentMethod.MOMO);
        attempt2.setStatus(PaymentStatus.PENDING);
        attempt2.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        paymentRepository.saveAndFlush(attempt2);

        Page<Payment> results = paymentRepository.findByBookingId(200L, PageRequest.of(0, 10));
        assertEquals(2, results.getTotalElements());
    }

    @Test
    public void testUniqueConstraintsAndNullBehavior() {
        Payment payment1 = new Payment();
        payment1.setPaymentTransactionCode("PAY-U01");
        payment1.setBookingId(300L);
        payment1.setAmount(new BigDecimal("100.00"));
        payment1.setPaymentMethod(PaymentMethod.VNPAY);
        payment1.setStatus(PaymentStatus.PENDING);
        payment1.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        // Multiple NULL external id allowed
        payment1.setExternalTransactionId(null);
        paymentRepository.saveAndFlush(payment1);

        Payment payment2 = new Payment();
        payment2.setPaymentTransactionCode("PAY-U02");
        payment2.setBookingId(301L);
        payment2.setAmount(new BigDecimal("100.00"));
        payment2.setPaymentMethod(PaymentMethod.VNPAY);
        payment2.setStatus(PaymentStatus.PENDING);
        payment2.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        payment2.setExternalTransactionId(null);
        paymentRepository.saveAndFlush(payment2); // Should succeed

        Payment payment3 = new Payment();
        payment3.setPaymentTransactionCode("PAY-U01"); // Duplicate code
        payment3.setBookingId(302L);
        payment3.setAmount(new BigDecimal("100.00"));
        payment3.setPaymentMethod(PaymentMethod.VNPAY);
        payment3.setStatus(PaymentStatus.PENDING);
        payment3.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        assertThrows(DataIntegrityViolationException.class, () -> {
            paymentRepository.saveAndFlush(payment3);
        });
    }

    @Test
    public void testOptimisticLockingFailure() {
        Payment payment = new Payment();
        payment.setPaymentTransactionCode("PAY-LOCK");
        payment.setBookingId(400L);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        
        payment = paymentRepository.saveAndFlush(payment);

        // Fetch tx1 and immediately detach it to simulate a concurrent transaction
        Payment tx1 = paymentRepository.findById(payment.getId()).get();
        entityManager.detach(tx1);

        // Fetch tx2 (this one remains managed)
        Payment tx2 = paymentRepository.findById(payment.getId()).get();

        // tx2 updates the entity and flushes, incrementing the version in DB
        tx2.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.saveAndFlush(tx2);

        // tx1 (detached, holding the old version) tries to update
        tx1.setStatus(PaymentStatus.FAILED);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            paymentRepository.saveAndFlush(tx1);
        });
    }

    @Test
    public void testActiveAndExpiredQueries() {
        LocalDateTime now = LocalDateTime.now();

        Payment active = new Payment();
        active.setPaymentTransactionCode("PAY-ACT");
        active.setBookingId(500L);
        active.setAmount(new BigDecimal("50.00"));
        active.setPaymentMethod(PaymentMethod.MOMO);
        active.setStatus(PaymentStatus.PENDING);
        active.setExpiresAt(now.plusMinutes(10)); // Future expiration
        paymentRepository.saveAndFlush(active);

        Payment expired = new Payment();
        expired.setPaymentTransactionCode("PAY-EXP");
        expired.setBookingId(501L);
        expired.setAmount(new BigDecimal("50.00"));
        expired.setPaymentMethod(PaymentMethod.MOMO);
        expired.setStatus(PaymentStatus.PENDING);
        expired.setExpiresAt(now.minusMinutes(10)); // Past expiration
        paymentRepository.saveAndFlush(expired);

        List<Payment> foundActive = paymentRepository.findActiveAttempts(
                500L, Arrays.asList(PaymentStatus.PENDING), now);
        assertFalse(foundActive.isEmpty());
        assertEquals("PAY-ACT", foundActive.get(0).getPaymentTransactionCode());

        Page<Payment> foundExpired = paymentRepository.findExpiredActivePayments(
                Arrays.asList(PaymentStatus.PENDING), now, PageRequest.of(0, 10));
        assertEquals(1, foundExpired.getTotalElements());
        assertEquals("PAY-EXP", foundExpired.getContent().get(0).getPaymentTransactionCode());
    }
}
