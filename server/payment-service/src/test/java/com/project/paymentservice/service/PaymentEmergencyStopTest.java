package com.project.paymentservice.service;

import com.project.paymentservice.entity.BookingPaymentGuard;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.enumtype.PaymentMethod;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.repository.BookingPaymentGuardRepository;
import com.project.paymentservice.repository.CashPaymentDetailRepository;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentIdempotencyRecordRepository;
import com.project.paymentservice.repository.PaymentReconciliationCaseRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEmergencyStopTest {

    @Mock PaymentRepository paymentRepository;
    @Mock BookingPaymentGuardRepository guardRepository;
    @Mock PaymentAnalyticsSnapshotRepository snapshotRepository;
    @Mock CashPaymentDetailRepository cashRepository;
    @Mock PaymentReconciliationCaseRepository reconciliationRepository;
    @Mock PaymentIdempotencyRecordRepository idempotencyRepository;
    @Mock TransactionCodeGenerator codeGenerator;
    @Mock PaymentLogService logService;
    @Mock PaymentOutboxService outboxService;
    @Mock RefundService refundService;

    private PaymentTransactionService service;

    @BeforeEach
    void setUp() {
        service = new PaymentTransactionService(
                paymentRepository, guardRepository, snapshotRepository, cashRepository,
                reconciliationRepository, idempotencyRepository, codeGenerator,
                logService, outboxService, new PaymentStateTransitionService(), refundService);
    }

    @Test
    void stopsPendingAndProcessingAttemptsButReportsAlreadySuccessfulBookings() {
        Payment pending = payment(1L, "booking-pending", PaymentStatus.PENDING);
        Payment processing = payment(2L, "booking-processing", PaymentStatus.PROCESSING);
        Payment success = payment(3L, "booking-success", PaymentStatus.SUCCESS);
        BookingPaymentGuard pendingGuard = guard("booking-pending", 1L);
        BookingPaymentGuard processingGuard = guard("booking-processing", 2L);

        when(paymentRepository.findByBookingPublicIdInForEmergencyUpdate(
                List.of("booking-pending", "booking-processing", "booking-success")))
                .thenReturn(List.of(pending, processing, success));
        when(guardRepository.findByBookingPublicIdForUpdate("booking-pending"))
                .thenReturn(Optional.of(pendingGuard));
        when(guardRepository.findByBookingPublicIdForUpdate("booking-processing"))
                .thenReturn(Optional.of(processingGuard));

        var result = service.stopActiveAttemptsForEmergency(
                List.of("booking-pending", "booking-processing", "booking-success"),
                "Phòng chiếu đóng khẩn cấp");

        assertThat(result.stoppedPaymentAttemptCount()).isEqualTo(2);
        assertThat(result.alreadySuccessfulBookingPublicIds()).containsExactly("booking-success");
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(success.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(pendingGuard.getActivePaymentId()).isNull();
        assertThat(processingGuard.getActivePaymentId()).isNull();
        verify(outboxService).enqueueBookingResult(pending, "CANCELLED", pending.getCancelledAt());
        verify(outboxService).enqueueBookingResult(processing, "CANCELLED", processing.getCancelledAt());
    }

    @Test
    void assessesActiveAndSuccessfulPaymentsWithoutMutatingThem() {
        Payment pending = payment(1L, "booking-pending", PaymentStatus.PENDING);
        Payment processing = payment(2L, "booking-processing", PaymentStatus.PROCESSING);
        Payment success = payment(3L, "booking-success", PaymentStatus.SUCCESS);
        Payment failed = payment(4L, "booking-failed", PaymentStatus.FAILED);
        List<String> bookingIds = List.of(
                "booking-pending", "booking-processing", "booking-success", "booking-failed");
        when(paymentRepository.findByBookingPublicIdIn(bookingIds))
                .thenReturn(List.of(pending, processing, success, failed));

        var result = service.assessPaymentsForEmergency(bookingIds);

        assertThat(result.activePaymentBookingPublicIds())
                .containsExactly("booking-pending", "booking-processing");
        assertThat(result.successfulPaymentBookingPublicIds())
                .containsExactly("booking-success");
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(processing.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(success.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    private Payment payment(Long id, String bookingPublicId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setPublicId("payment-" + id);
        payment.setBookingPublicId(bookingPublicId);
        payment.setStatus(status);
        payment.setAmount(new BigDecimal("180000"));
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setProviderCode(ProviderCode.VNPAY);
        return payment;
    }

    private BookingPaymentGuard guard(String bookingPublicId, Long paymentId) {
        BookingPaymentGuard guard = new BookingPaymentGuard();
        guard.setBookingPublicId(bookingPublicId);
        guard.setActivePaymentId(paymentId);
        return guard;
    }
}
