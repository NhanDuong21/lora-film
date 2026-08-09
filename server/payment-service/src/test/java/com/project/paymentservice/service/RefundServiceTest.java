package com.project.paymentservice.service;

import com.project.paymentservice.config.PaymentRuntimeProperties;
import com.project.paymentservice.dto.request.CreateRefundRequest;
import com.project.paymentservice.entity.Payment;
import com.project.paymentservice.entity.PaymentAnalyticsSnapshot;
import com.project.paymentservice.entity.PaymentRefund;
import com.project.paymentservice.enumtype.PaymentStatus;
import com.project.paymentservice.enumtype.ProviderCode;
import com.project.paymentservice.enumtype.RefundComponent;
import com.project.paymentservice.enumtype.RefundStatus;
import com.project.paymentservice.enumtype.RefundType;
import com.project.paymentservice.exception.BusinessException;
import com.project.paymentservice.provider.ProviderRefundResult;
import com.project.paymentservice.repository.PaymentAnalyticsSnapshotRepository;
import com.project.paymentservice.repository.PaymentRefundRepository;
import com.project.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentRefundRepository refundRepository;
    @Mock
    private PaymentAnalyticsSnapshotRepository snapshotRepository;
    @Mock
    private PaymentOutboxService outboxService;

    private RefundService service;
    private PaymentRuntimeProperties runtimeProperties;
    private Payment payment;

    @BeforeEach
    void setUp() {
        runtimeProperties = new PaymentRuntimeProperties();
        service = new RefundService(
                paymentRepository,
                refundRepository,
                snapshotRepository,
                outboxService,
                runtimeProperties);
        payment = new Payment();
        payment.setId(10L);
        payment.setPublicId("payment-public-id");
        payment.setBookingPublicId("booking-public-id");
        payment.setProviderCode(ProviderCode.VNPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(new BigDecimal("100000.00"));
        payment.setCurrency("VND");
        lenient().when(paymentRepository.findByPublicIdForUpdate(payment.getPublicId()))
                .thenReturn(Optional.of(payment));
        lenient().when(refundRepository.findByPaymentIdAndRequestKey(eq(payment.getId()), any()))
                .thenReturn(Optional.empty());
        lenient().when(refundRepository.saveAndFlush(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void fullRemainingAfterPreviousPartialUsesSafeProviderPartialOperation() {
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(new BigDecimal("30000.00"));

        var response = service.createAdminRefund(
                payment.getPublicId(),
                "refund-key",
                99L,
                request(RefundType.FULL, RefundComponent.FULL_ORDER, null));

        assertEquals(new BigDecimal("70000.00"), response.getAmount());
        assertEquals(RefundType.PARTIAL.name(), response.getRefundType());
        assertEquals(RefundComponent.FULL_ORDER.name(), response.getRefundComponent());
    }

    @Test
    void concessionRefundCannotExceedImmutableFoodSnapshot() {
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);
        when(refundRepository.sumReservedAmountByComponent(
                eq(payment.getId()), eq(RefundComponent.CONCESSION), anyCollection()))
                .thenReturn(new BigDecimal("10000.00"));
        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setFoodAmount(new BigDecimal("30000.00"));
        when(snapshotRepository.findByPaymentId(payment.getId()))
                .thenReturn(Optional.of(snapshot));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdminRefund(
                        payment.getPublicId(),
                        "food-key",
                        99L,
                        request(
                                RefundType.PARTIAL,
                                RefundComponent.CONCESSION,
                                new BigDecimal("25000.00"))));

        assertEquals("REFUND_AMOUNT_EXCEEDS_AVAILABLE", exception.getErrorCode());
        verify(refundRepository, never()).saveAndFlush(any(PaymentRefund.class));
    }

    @Test
    void individualTicketRefundShapeIsExplicitlyRejected() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createAdminRefund(
                        payment.getPublicId(),
                        "ticket-key",
                        99L,
                        request(
                                RefundType.PARTIAL,
                                RefundComponent.FULL_ORDER,
                                new BigDecimal("50000.00"))));

        assertEquals("TICKET_REFUND_NOT_SUPPORTED", exception.getErrorCode());
        verify(refundRepository, never()).sumReservedAmount(any(), anyCollection());
    }

    @Test
    void automaticFullRefundIsIdempotentByStableReasonKey() {
        when(paymentRepository.findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);

        PaymentRefund first = service.createAutomaticFullRefund(
                payment.getId(),
                "late-success:provider-order-1",
                "LATE_PROVIDER_SUCCESS",
                "Provider success after deadline");
        when(refundRepository.findByPaymentIdAndRequestKey(
                payment.getId(), "late-success:provider-order-1"))
                .thenReturn(Optional.of(first));

        PaymentRefund replay = service.createAutomaticFullRefund(
                payment.getId(),
                "late-success:provider-order-1",
                "LATE_PROVIDER_SUCCESS",
                "Provider success after deadline");

        assertTrue(first == replay);
        ArgumentCaptor<PaymentRefund> captor = ArgumentCaptor.forClass(PaymentRefund.class);
        verify(refundRepository).saveAndFlush(captor.capture());
        assertTrue(captor.getValue().isAutomatic());
        assertEquals(RefundComponent.FULL_ORDER, captor.getValue().getRefundComponent());
    }

    @Test
    void cancelledShowtimeCreatesAutomaticFullRefundForSuccessfulPayments() {
        when(snapshotRepository.findSuccessfulPaymentIdsByShowtimePublicId(
                "showtime-public-id")).thenReturn(List.of(payment.getId()));
        when(paymentRepository.findByIdForUpdate(payment.getId()))
                .thenReturn(Optional.of(payment));
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);

        int created = service.createShowtimeCancellationRefunds(
                "showtime-public-id",
                "showtime-cancelled-event",
                "Rạp hủy suất chiếu");

        assertEquals(1, created);
        ArgumentCaptor<PaymentRefund> captor = ArgumentCaptor.forClass(PaymentRefund.class);
        verify(refundRepository).saveAndFlush(captor.capture());
        PaymentRefund refund = captor.getValue();
        assertTrue(refund.isAutomatic());
        assertEquals(RefundType.FULL, refund.getRefundType());
        assertEquals(RefundComponent.FULL_ORDER, refund.getRefundComponent());
        assertEquals("SHOWTIME_CANCELLED", refund.getReasonCode());
        assertEquals(payment.getAmount(), refund.getRequestedAmount());
    }

    @Test
    void employeeRequestWaitsForManagerApprovalBeforeProviderProcessing() {
        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setCinemaPublicId("cinema-a");
        when(snapshotRepository.findByPaymentId(payment.getId())).thenReturn(Optional.of(snapshot));
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);

        var created = service.createEmployeeRefundRequest(
                payment.getPublicId(),
                "employee-refund-key",
                31L,
                "cinema-a",
                request(RefundType.FULL, RefundComponent.FULL_ORDER, null));

        assertEquals(RefundStatus.PENDING_APPROVAL.name(), created.getStatus());
        assertEquals(31L, created.getRequestedByAccountId());
        PaymentRefund refund = captureLastSavedRefund();
        assertEquals(null, refund.getNextAttemptAt());

        when(refundRepository.findByPublicId(refund.getPublicId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.findByIdForUpdate(refund.getId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var approved = service.approve(
                refund.getPublicId(), "cinema-a", 2L, "Đã xác minh tại rạp");

        assertEquals(RefundStatus.REQUESTED.name(), approved.getStatus());
        assertEquals(2L, approved.getReviewedByAccountId());
        assertEquals("Đã xác minh tại rạp", approved.getReviewNote());
        assertTrue(refund.getNextAttemptAt() != null);
    }

    @Test
    void employeeCannotCreateRefundForAnotherCinema() {
        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setCinemaPublicId("cinema-b");
        when(snapshotRepository.findByPaymentId(payment.getId())).thenReturn(Optional.of(snapshot));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createEmployeeRefundRequest(
                        payment.getPublicId(),
                        "wrong-cinema-key",
                        31L,
                        "cinema-a",
                        request(RefundType.FULL, RefundComponent.FULL_ORDER, null)));

        assertEquals("REFUND_CINEMA_SCOPE_DENIED", exception.getErrorCode());
        verify(refundRepository, never()).saveAndFlush(any(PaymentRefund.class));
    }

    @Test
    void cashRefundRequiresManualSettlementAndCompletesExactlyOnce() {
        payment.setProviderCode(ProviderCode.CASH);
        when(refundRepository.sumReservedAmount(eq(payment.getId()), anyCollection()))
                .thenReturn(BigDecimal.ZERO);
        var refund = service.createAdminRefund(
                payment.getPublicId(),
                "cash-key",
                99L,
                request(RefundType.FULL, RefundComponent.FULL_ORDER, null));
        assertEquals(RefundStatus.REQUIRES_ACTION.name(), refund.getStatus());

        PaymentRefund entity = captureLastSavedRefund();
        when(refundRepository.findByPublicId(entity.getPublicId()))
                .thenReturn(Optional.of(entity));
        when(refundRepository.findByIdForUpdate(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var completed = service.completeCashRefund(
                entity.getPublicId(), 99L, "CASH-RECEIPT-001", "Đã trả cho khách");
        var replay = service.completeCashRefund(
                entity.getPublicId(), 99L, "CASH-RECEIPT-001", "Đã trả cho khách");

        assertEquals(RefundStatus.SUCCESS.name(), completed.getStatus());
        assertEquals(completed.getRefundPublicId(), replay.getRefundPublicId());
        verify(outboxService).enqueueBookingRefundResult(eq(entity), eq(true), any());
    }

    @Test
    void employeeCompletesApprovedCashRefundOnlyInsideAssignedCinema() {
        payment.setProviderCode(ProviderCode.CASH);
        PaymentRefund refund = new PaymentRefund();
        refund.setId(81L);
        refund.setPublicId("cash-refund-public-id");
        refund.setRefundCode("RFD-CASH-001");
        refund.setPayment(payment);
        refund.setProviderCode(ProviderCode.CASH);
        refund.setRefundType(RefundType.FULL);
        refund.setRefundComponent(RefundComponent.FULL_ORDER);
        refund.setReasonCode("SHOWTIME_CANCELLED");
        refund.setStatus(RefundStatus.REQUIRES_ACTION);
        refund.setReasonDetailSanitized("Suất chiếu bị hủy");
        refund.setRequestedAmount(new BigDecimal("75000.00"));
        refund.setCurrency("VND");

        PaymentAnalyticsSnapshot snapshot = new PaymentAnalyticsSnapshot();
        snapshot.setCinemaPublicId("cinema-a");
        when(snapshotRepository.findByPaymentId(payment.getId())).thenReturn(Optional.of(snapshot));
        when(refundRepository.findByPublicId(refund.getPublicId())).thenReturn(Optional.of(refund));
        when(refundRepository.findByIdForUpdate(refund.getId())).thenReturn(Optional.of(refund));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException denied = assertThrows(
                BusinessException.class,
                () -> service.completeEmployeeCashRefund(
                        refund.getPublicId(), 31L, "cinema-b",
                        "PC-001", "Đã giao đủ tiền cho khách"));
        assertEquals("REFUND_CINEMA_SCOPE_DENIED", denied.getErrorCode());

        var completed = service.completeEmployeeCashRefund(
                refund.getPublicId(), 31L, "cinema-a",
                "PC-001", "Đã giao đủ tiền cho khách");

        assertEquals(RefundStatus.SUCCESS.name(), completed.getStatus());
        assertEquals("PC-001", completed.getProviderRefundId());
        verify(outboxService).enqueueBookingRefundResult(eq(refund), eq(true), any());
    }

    @Test
    void providerProcessingStatusDoesNotEscalateByTechnicalRetryCount() {
        PaymentRefund refund = leasedProcessingRefund(7, Instant.now().minusSeconds(30));
        when(refundRepository.findByIdForUpdate(refund.getId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.PROCESSING);
        result.setRetryAfterSeconds(305);
        result.setFailureCode("VNPAY_REFUND_PROCESSING_05");
        result.setMessageSanitized("VNPay đang xử lý yêu cầu hoàn tiền");

        Instant beforeApply = Instant.now();
        service.applyProviderResult(refund.getId(), "refund-worker", result);

        assertEquals(RefundStatus.PROCESSING, refund.getStatus());
        assertEquals(8, refund.getRetryCount());
        assertTrue(refund.getNextAttemptAt()
                .isAfter(beforeApply.plusSeconds(300)));
        assertEquals("VNPAY_REFUND_PROCESSING_05", refund.getFailureCode());
        verify(outboxService, never()).enqueueBookingRefundResult(
                any(), anyBoolean(), any());
    }

    @Test
    void providerProcessingEscalatesOnlyAfterConfiguredOperationalSla() {
        runtimeProperties.setRefundProcessingMaxAgeHours(1);
        PaymentRefund refund = leasedProcessingRefund(
                1, Instant.now().minusSeconds(3601));
        when(refundRepository.findByIdForUpdate(refund.getId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProviderRefundResult result = new ProviderRefundResult();
        result.setState(ProviderRefundResult.State.PROCESSING);
        result.setFailureCode("VNPAY_REFUND_PROCESSING_06");
        result.setMessageSanitized("VNPay đã chuyển yêu cầu hoàn tiền sang ngân hàng");

        service.applyProviderResult(refund.getId(), "refund-worker", result);

        assertEquals(RefundStatus.REQUIRES_ACTION, refund.getStatus());
        assertEquals(null, refund.getNextAttemptAt());
        verify(outboxService, never()).enqueueBookingRefundResult(
                any(), anyBoolean(), any());
    }

    @Test
    void retryOfPreviouslySubmittedUncertainRefundQueriesInsteadOfResubmitting() {
        PaymentRefund refund = leasedProcessingRefund(
                8, Instant.now().minusSeconds(600));
        refund.setProviderRefundId("provider-refund-id");
        refund.setStatus(RefundStatus.REQUIRES_ACTION);
        refund.setProviderCode(ProviderCode.VNPAY);
        refund.setRefundType(RefundType.PARTIAL);
        refund.setRefundComponent(RefundComponent.CONCESSION);
        refund.setReasonCode("OPERATIONAL_ADJUSTMENT");
        refund.setRequestedAmount(new BigDecimal("10000.00"));
        refund.setCurrency("VND");
        when(refundRepository.findByPublicId(refund.getPublicId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.findByIdForUpdate(refund.getId()))
                .thenReturn(Optional.of(refund));
        when(refundRepository.save(any(PaymentRefund.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.retry(refund.getPublicId());

        assertEquals(RefundStatus.PROCESSING.name(), response.getStatus());
        assertEquals(RefundStatus.PROCESSING, refund.getStatus());
        assertEquals(0, refund.getRetryCount());
        assertTrue(refund.getNextAttemptAt() != null);
    }

    @Test
    void processingRefundQueriesAfterAnyProviderResponseWasObserved() {
        PaymentRefund refund = leasedProcessingRefund(
                1, Instant.now().minusSeconds(60));
        when(refundRepository.findByIdForUpdate(refund.getId()))
                .thenReturn(Optional.of(refund));
        when(paymentRepository.findById(payment.getId()))
                .thenReturn(Optional.of(payment));

        RefundService.RefundWork work =
                service.loadOwnedWork(refund.getId(), "refund-worker");

        assertFalse(work.queryOnly());

        refund.setProviderResponseCode("94");
        RefundService.RefundWork acknowledgedWithoutProviderRefundId =
                service.loadOwnedWork(refund.getId(), "refund-worker");
        assertTrue(acknowledgedWithoutProviderRefundId.queryOnly());

        refund.setProviderResponseCode(null);
        refund.setProviderRefundId("provider-refund-id");
        RefundService.RefundWork knownProviderRefund =
                service.loadOwnedWork(refund.getId(), "refund-worker");
        assertTrue(knownProviderRefund.queryOnly());
    }

    private PaymentRefund leasedProcessingRefund(
            int retryCount,
            Instant submittedAt) {
        PaymentRefund refund = new PaymentRefund();
        refund.setId(20L);
        refund.setPublicId("refund-public-id");
        refund.setRefundCode("RFD-0001");
        refund.setPayment(payment);
        refund.setStatus(RefundStatus.PROCESSING);
        refund.setRetryCount(retryCount);
        refund.setRequestedAt(submittedAt.minusSeconds(30));
        refund.setSubmittedAt(submittedAt);
        refund.setLockedBy("refund-worker");
        refund.setLockedUntil(Instant.now().plusSeconds(60));
        return refund;
    }

    private PaymentRefund captureLastSavedRefund() {
        ArgumentCaptor<PaymentRefund> captor = ArgumentCaptor.forClass(PaymentRefund.class);
        verify(refundRepository).saveAndFlush(captor.capture());
        PaymentRefund refund = captor.getValue();
        refund.setId(20L);
        return refund;
    }

    private CreateRefundRequest request(
            RefundType type,
            RefundComponent component,
            BigDecimal amount) {
        CreateRefundRequest request = new CreateRefundRequest();
        request.setRefundType(type);
        request.setRefundComponent(component);
        request.setAmount(amount);
        request.setReasonCode(type == RefundType.FULL
                ? "CUSTOMER_SERVICE_APPROVED" : "OPERATIONAL_ADJUSTMENT");
        request.setNote("Đã kiểm tra và được phê duyệt");
        return request;
    }
}
