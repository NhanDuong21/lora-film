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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    private Payment payment;

    @BeforeEach
    void setUp() {
        service = new RefundService(
                paymentRepository,
                refundRepository,
                snapshotRepository,
                outboxService,
                new PaymentRuntimeProperties());
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
