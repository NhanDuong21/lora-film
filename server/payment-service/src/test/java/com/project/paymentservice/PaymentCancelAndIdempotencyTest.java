package com.project.paymentservice;

import com.project.paymentservice.dto.response.CancelPaymentResponse;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
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
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentCancelAndIdempotencyTest {

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
    void cancelPaymentWithSameKeyShouldReplayResponse() {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("CAN-123");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        p = paymentRepository.save(p);

        // First cancel
        CancelPaymentResponse resp1 = paymentService.cancelPayment(15L, "cancel-key", p.getId());
        
        // Second cancel with same key
        CancelPaymentResponse resp2 = paymentService.cancelPayment(15L, "cancel-key", p.getId());

        assertNotNull(resp1);
        assertNotNull(resp2);
        assertEquals(resp1.getPaymentId(), resp2.getPaymentId());
        assertEquals(resp1.getStatus(), resp2.getStatus());
        assertEquals(PaymentStatus.CANCELLED.name(), resp2.getStatus());
    }
}
