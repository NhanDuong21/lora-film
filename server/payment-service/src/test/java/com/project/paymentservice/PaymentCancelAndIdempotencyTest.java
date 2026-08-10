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
import com.project.paymentservice.service.PaymentIdempotencyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

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

    @Autowired
    private PaymentIdempotencyService paymentIdempotencyService;

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
    void cancelPaymentWithSameKeyShouldReplayResponse() {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("CAN-123");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(Instant.now().plusSeconds(900));
        p = savePaymentWithGuard(p);

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

    @Test
    void retryAfterFailedCancelShouldNotRemainInProgressForever() {
        Payment p = new Payment();
        p.setAccountId(15L);
        p.setBookingId(1001L);
        p.setPaymentTransactionCode("CAN-RETRY");
        p.setAmount(new BigDecimal("100"));
        p.setPaymentMethod(PaymentMethod.MOCK);
        p.setAttemptNumber(1);
        p.setStatus(PaymentStatus.PENDING);
        p.setExpiresAt(Instant.now().plusSeconds(900));
        final Payment savedP = savePaymentWithGuard(p);

        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CANCEL_PAYMENT");
        record.setIdempotencyKey("failed-cancel-key");
        record.setRequestHash(com.project.paymentservice.service.CanonicalHashUtil.hashOperation(
                "CANCEL_PAYMENT", 15L, savedP.getPublicId()));
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED);
        record.setErrorCode("INTERNAL_SERVER_ERROR");
        record.setLastError("Simulated failure");
        record.setResponseStatus(500);
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        com.project.paymentservice.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.project.paymentservice.exception.BusinessException.class,
                () -> paymentService.cancelPayment(15L, "failed-cancel-key", savedP.getId())
        );

        assertEquals("INTERNAL_SERVER_ERROR", ex.getErrorCode());
        assertEquals("Simulated failure", ex.getMessage());

        Payment after = paymentRepository.findById(savedP.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, after.getStatus());
        assertEquals(0, logRepository.count());
    }

    @Test
    void cancelPaymentWithSameKeyDifferentHashShouldConflict() {
        Payment p1 = new Payment();
        p1.setAccountId(15L);
        p1.setBookingId(1001L);
        p1.setPaymentTransactionCode("CAN-DIFF-1");
        p1.setAmount(new BigDecimal("100"));
        p1.setPaymentMethod(PaymentMethod.MOCK);
        p1.setAttemptNumber(1);
        p1.setStatus(PaymentStatus.PENDING);
        p1.setExpiresAt(Instant.now().plusSeconds(900));
        p1 = savePaymentWithGuard(p1);

        Payment p2 = new Payment();
        p2.setAccountId(15L);
        p2.setBookingId(1002L);
        p2.setPaymentTransactionCode("CAN-DIFF-2");
        p2.setAmount(new BigDecimal("200"));
        p2.setPaymentMethod(PaymentMethod.MOCK);
        p2.setAttemptNumber(1);
        p2.setStatus(PaymentStatus.PENDING);
        p2.setExpiresAt(Instant.now().plusSeconds(900));
        final Payment savedP2 = savePaymentWithGuard(p2);

        // First cancel
        paymentService.cancelPayment(15L, "shared-cancel-key", p1.getId());

        // Second cancel for DIFFERENT payment but SAME key
        com.project.paymentservice.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.project.paymentservice.exception.BusinessException.class,
                () -> paymentService.cancelPayment(15L, "shared-cancel-key", savedP2.getId())
        );

        assertEquals("IDEMPOTENCY_KEY_REUSED", ex.getErrorCode());
        Payment afterP2 = paymentRepository.findById(savedP2.getId()).orElseThrow();
        assertEquals(PaymentStatus.PENDING, afterP2.getStatus());
    }

    @Test
    void idempotencyKeyReuseConflictShouldNotMutateExistingRecord() {
        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey("conflict-key");
        record.setRequestHash("hash-A");
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.PROCESSING);
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        com.project.paymentservice.dto.request.CreatePaymentRequest req = new com.project.paymentservice.dto.request.CreatePaymentRequest(9999L, "MOCK");

        com.project.paymentservice.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.project.paymentservice.exception.BusinessException.class,
                () -> paymentService.createPayment(15L, "conflict-key", req)
        );

        assertEquals("IDEMPOTENCY_KEY_REUSED", ex.getErrorCode());
        
        var after = idempotencyRepository.findById(record.getId()).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.PROCESSING, after.getProcessingStatus());
        assertEquals("hash-A", after.getRequestHash());
    }

    @Test
    void completedReplayShouldNotBeMarkedFailed() {
        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey("replay-completed-key");
        String hash = com.project.paymentservice.service.CanonicalHashUtil.hashCreatePayment(15L, 8888L, "MOCK");
        record.setRequestHash(hash);
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.COMPLETED);
        record.setResponseStatus(200);
        record.setResponseBodySanitized("{\"success\":true}");
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        com.project.paymentservice.dto.request.CreatePaymentRequest req = new com.project.paymentservice.dto.request.CreatePaymentRequest(8888L, "MOCK");
        
        // This should return replay response without error
        paymentService.createPayment(15L, "replay-completed-key", req);

        var after = idempotencyRepository.findById(record.getId()).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.COMPLETED, after.getProcessingStatus());
        assertEquals(0, paymentRepository.count());
    }

    @Test
    void failedReplayShouldNotBeMarkedFailedAgain() {
        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey("replay-failed-key");
        String hash = com.project.paymentservice.service.CanonicalHashUtil.hashCreatePayment(15L, 7777L, "MOCK");
        record.setRequestHash(hash);
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED);
        record.setErrorCode("SOME_ERROR");
        record.setLastError("Some error msg");
        record.setResponseStatus(400);
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        com.project.paymentservice.dto.request.CreatePaymentRequest req = new com.project.paymentservice.dto.request.CreatePaymentRequest(7777L, "MOCK");
        
        com.project.paymentservice.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.project.paymentservice.exception.BusinessException.class,
                () -> paymentService.createPayment(15L, "replay-failed-key", req)
        );

        assertEquals("SOME_ERROR", ex.getErrorCode());
        
        var after = idempotencyRepository.findById(record.getId()).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.FAILED, after.getProcessingStatus());
        assertEquals(0, paymentRepository.count());
    }

    @Test
    void staleFailureFinalizationShouldNotOverwriteCompletedRecord() {
        com.project.paymentservice.entity.PaymentIdempotencyRecord record = new com.project.paymentservice.entity.PaymentIdempotencyRecord();
        record.setAccountId(15L);
        record.setOperation("CREATE_PAYMENT");
        record.setIdempotencyKey("stale-fail-key");
        record.setRequestHash("hash-stale");
        record.setProcessingStatus(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.COMPLETED);
        record.setResponseStatus(200);
        record.setResponseBodySanitized("{\"success\":true}");
        record.setExpiresAt(Instant.now().plusSeconds(3600));
        idempotencyRepository.saveAndFlush(record);

        com.project.paymentservice.exception.BusinessException staleOwner =
                org.junit.jupiter.api.Assertions.assertThrows(
                        com.project.paymentservice.exception.BusinessException.class,
                        () -> paymentIdempotencyService.fail(
                                record.getId(), "stale-owner", "STALE_ERR", "Stale error"));
        assertEquals("IDEMPOTENCY_OWNER_MISMATCH", staleOwner.getErrorCode());

        var after = idempotencyRepository.findById(record.getId()).orElseThrow();
        assertEquals(com.project.paymentservice.enumtype.IdempotencyProcessingStatus.COMPLETED, after.getProcessingStatus());
        assertEquals(200, after.getResponseStatus());
        org.junit.jupiter.api.Assertions.assertNull(after.getErrorCode());
    }

    private Payment savePaymentWithGuard(Payment payment) {
        Payment saved = paymentRepository.saveAndFlush(TestFixtures.complete(payment));
        guardRepository.saveAndFlush(TestFixtures.guard(saved));
        return saved;
    }
}
