package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionConfirmationReconciliationServiceTest {

    private final BookingRepository bookingRepository = mock(BookingRepository.class);
    private final BookingReconciliationTaskRepository taskRepository =
            mock(BookingReconciliationTaskRepository.class);
    private final BookingMetricsManager metricsManager =
            mock(BookingMetricsManager.class);
    private final PromotionConfirmationReconciliationService service =
            new PromotionConfirmationReconciliationService(
                    bookingRepository, taskRepository, metricsManager, 1);

    @BeforeEach
    void persistTasksByReference() {
        when(taskRepository.saveAndFlush(any(BookingReconciliationTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void matchingConfirmedBookingIsMarkedMatched() {
        Booking booking = booking(
                "booking-1", "reservation-1", BookingStatus.CONFIRMED);
        stubNewObservation(booking, "payment-1");

        service.observeLifecycleEvent(
                "reservation-1", "booking-1", "payment-1",
                "RESERVATION_CONFIRMED");

        BookingReconciliationTask task = savedTask();
        assertThat(task.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MATCHED);
        assertThat(task.getReason())
                .isEqualTo("PROMOTION_CONFIRMED_AWAITING_BOOKING_CONFIRMATION");
        assertThat(task.getCheckedAt()).isNotNull();
    }

    @Test
    void confirmedPromotionWaitsWhileBookingIsNotConfirmed() {
        Booking booking = booking(
                "booking-2", "reservation-2", BookingStatus.PENDING_PAYMENT);
        stubNewObservation(booking, "payment-2");

        service.observeLifecycleEvent(
                "reservation-2", "booking-2", "payment-2",
                "RESERVATION_CONFIRMED");

        BookingReconciliationTask task = savedTask();
        assertThat(task.getReconciliationStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(task.getCheckedAt()).isNull();
    }

    @Test
    void reservationIdMismatchCanNeverBecomeMatchedDuringRecheck() {
        Booking booking = booking(
                "booking-3", "expected-reservation", BookingStatus.CONFIRMED);
        stubNewObservation(booking, "payment-3");

        service.observeLifecycleEvent(
                "different-reservation", "booking-3", "payment-3",
                "RESERVATION_CONFIRMED");
        BookingReconciliationTask task = savedTask();
        when(taskRepository.findPromotionTasksForRecheck(
                anyCollection(), eq("PROMOTION_"), any(Instant.class), any()))
                .thenReturn(List.of(task));
        when(taskRepository.countByReconciliationStatusAndReasonStartingWith(
                ReconciliationStatus.MISMATCH, "PROMOTION_"))
                .thenReturn(1L);

        service.recheckObservedPromotionTransitions();

        assertThat(task.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);
        assertThat(task.getReason()).isEqualTo("PROMOTION_RESERVATION_ID_MISMATCH");
        assertThat(task.getCheckedAt()).isNotNull();
        verify(metricsManager).incrementPromotionReconciliationMismatch();
        verify(metricsManager).updatePromotionReconciliationMismatch(1);
    }

    @Test
    void unresolvedMismatchIsRescheduledSoItCannotStarveNewerTasks() {
        Instant previousCheck = Instant.parse("2026-08-01T08:00:00Z");
        BookingReconciliationTask task = new BookingReconciliationTask();
        task.setBooking(booking(
                "booking-4", "expected-reservation", BookingStatus.CONFIRMED));
        task.setReason("PROMOTION_RESERVATION_ID_MISMATCH");
        task.setReconciliationStatus(ReconciliationStatus.MISMATCH);
        task.setCheckedAt(previousCheck);
        when(taskRepository.findPromotionTasksForRecheck(
                anyCollection(), eq("PROMOTION_"), any(Instant.class), any()))
                .thenReturn(List.of(task));

        service.recheckObservedPromotionTransitions();

        assertThat(task.getReconciliationStatus()).isEqualTo(ReconciliationStatus.MISMATCH);
        assertThat(task.getCheckedAt()).isAfter(previousCheck);
        verify(taskRepository).save(task);
    }

    private void stubNewObservation(Booking booking, String paymentReference) {
        when(bookingRepository.findByPublicId(booking.getPublicId()))
                .thenReturn(Optional.of(booking));
        when(taskRepository
                .findFirstByBookingIdAndPaymentReferenceAndReasonStartingWithOrderByIdDesc(
                        booking.getId(), paymentReference, "PROMOTION_"))
                .thenReturn(Optional.empty());
    }

    private BookingReconciliationTask savedTask() {
        var captor = org.mockito.ArgumentCaptor
                .forClass(BookingReconciliationTask.class);
        verify(taskRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private Booking booking(
            String publicId,
            String reservationPublicId,
            BookingStatus status) {
        Booking booking = mock(Booking.class);
        when(booking.getId()).thenReturn(10L);
        when(booking.getPublicId()).thenReturn(publicId);
        when(booking.getPromotionReservationPublicId()).thenReturn(reservationPublicId);
        when(booking.getBookingStatus()).thenReturn(status);
        when(booking.getFinalAmount()).thenReturn(new BigDecimal("120000"));
        when(booking.getCurrency()).thenReturn("VND");
        return booking;
    }
}
