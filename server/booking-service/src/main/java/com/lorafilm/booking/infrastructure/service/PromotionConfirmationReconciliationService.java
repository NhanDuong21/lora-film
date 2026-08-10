package com.lorafilm.booking.infrastructure.service;

import com.lorafilm.booking.booking.entity.Booking;
import com.lorafilm.booking.booking.enums.BookingStatus;
import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.infrastructure.entity.BookingReconciliationTask;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import com.lorafilm.booking.infrastructure.monitoring.BookingMetricsManager;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

@Service
public class PromotionConfirmationReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(
            PromotionConfirmationReconciliationService.class);
    private static final String REASON_PREFIX = "PROMOTION_";
    private static final Duration SETTLEMENT_GRACE = Duration.ofMinutes(2);
    private static final int RECHECK_BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final BookingReconciliationTaskRepository taskRepository;
    private final BookingMetricsManager metricsManager;
    private final long mismatchAlertThreshold;
    private volatile boolean mismatchAlertActive;

    public PromotionConfirmationReconciliationService(
            BookingRepository bookingRepository,
            BookingReconciliationTaskRepository taskRepository,
            BookingMetricsManager metricsManager,
            @Value("${booking.promotion-reconciliation.mismatch-alert-threshold:1}")
            long mismatchAlertThreshold) {
        this.bookingRepository = bookingRepository;
        this.taskRepository = taskRepository;
        this.metricsManager = metricsManager;
        this.mismatchAlertThreshold = mismatchAlertThreshold;
    }

    @Transactional
    public String observeLifecycleEvent(
            String reservationPublicId,
            String bookingPublicId,
            String paymentReference,
            String eventType) {
        Booking booking = bookingRepository.findByPublicId(bookingPublicId)
                .orElseThrow(() -> new IllegalStateException(
                        "Booking was not found for promotion reconciliation"));
        String reason = reasonFor(eventType);
        BookingReconciliationTask task = taskRepository
                .findFirstByBookingIdAndPaymentReferenceAndReasonStartingWithOrderByIdDesc(
                        booking.getId(), paymentReference, REASON_PREFIX)
                .orElseGet(() -> newTask(booking, paymentReference));
        task.setExpectedAmount(booking.getFinalAmount());
        task.setActualAmount(booking.getFinalAmount());
        task.setExpectedCurrency(booking.getCurrency());
        task.setActualCurrency(booking.getCurrency());
        boolean reservationMatches = Objects.equals(
                booking.getPromotionReservationPublicId(), reservationPublicId);
        boolean settled = reservationMatches && localStateMatches(booking, eventType);
        task.setReconciliationStatus(settled
                ? ReconciliationStatus.MATCHED : ReconciliationStatus.PENDING);
        task.setReason(reservationMatches
                ? reason : "PROMOTION_RESERVATION_ID_MISMATCH");
        task.setCheckedAt(settled ? Instant.now() : null);
        return taskRepository.saveAndFlush(task).getPublicId();
    }

    @Scheduled(fixedDelayString =
            "${booking.promotion-reconciliation.fixed-delay-ms:60000}")
    @Transactional
    public void recheckObservedPromotionTransitions() {
        Instant now = Instant.now();
        var tasks = taskRepository.findPromotionTasksForRecheck(
                EnumSet.of(ReconciliationStatus.PENDING, ReconciliationStatus.MISMATCH),
                REASON_PREFIX, now.minus(SETTLEMENT_GRACE),
                PageRequest.of(0, RECHECK_BATCH_SIZE));
        for (BookingReconciliationTask task : tasks) {
            Booking booking = task.getBooking();
            boolean matched = localStateMatches(booking, task.getReason());
            ReconciliationStatus target = matched
                    ? ReconciliationStatus.MATCHED : ReconciliationStatus.MISMATCH;
            if (task.getReconciliationStatus() != target) {
                task.setReconciliationStatus(target);
                if (target == ReconciliationStatus.MISMATCH) {
                    metricsManager.incrementPromotionReconciliationMismatch();
                }
            }
            task.setCheckedAt(now);
            taskRepository.save(task);
        }
        updateMismatchMonitoring();
    }

    private BookingReconciliationTask newTask(
            Booking booking, String paymentReference) {
        BookingReconciliationTask task = new BookingReconciliationTask();
        task.setPublicId(UUID.randomUUID().toString());
        task.setBooking(booking);
        task.setPaymentReference(paymentReference);
        return task;
    }

    private String reasonFor(String eventType) {
        return switch (eventType) {
            case "RESERVATION_CONFIRMED" ->
                    "PROMOTION_CONFIRMED_AWAITING_BOOKING_CONFIRMATION";
            case "RESERVATION_REVERSED" ->
                    "PROMOTION_REVERSED_AWAITING_BOOKING_REFUND";
            default -> throw new IllegalArgumentException(
                    "Unsupported promotion lifecycle event: " + eventType);
        };
    }

    private boolean localStateMatches(Booking booking, String eventOrReason) {
        if ("PROMOTION_RESERVATION_ID_MISMATCH".equals(eventOrReason)) {
            return false;
        }
        if (eventOrReason.contains("REVERSED")) {
            return booking.getBookingStatus() == BookingStatus.REFUNDED;
        }
        return booking.getBookingStatus() == BookingStatus.CONFIRMED
                || booking.getBookingStatus() == BookingStatus.REFUNDED;
    }

    private void updateMismatchMonitoring() {
        long mismatchCount = taskRepository
                .countByReconciliationStatusAndReasonStartingWith(
                        ReconciliationStatus.MISMATCH, REASON_PREFIX);
        metricsManager.updatePromotionReconciliationMismatch(mismatchCount);
        boolean alert = mismatchCount >= mismatchAlertThreshold;
        if (alert && !mismatchAlertActive) {
            log.warn("Promotion reconciliation mismatch alert activated: count={}",
                    mismatchCount);
        } else if (!alert && mismatchAlertActive) {
            log.info("Promotion reconciliation mismatch alert resolved");
        }
        mismatchAlertActive = alert;
    }
}
