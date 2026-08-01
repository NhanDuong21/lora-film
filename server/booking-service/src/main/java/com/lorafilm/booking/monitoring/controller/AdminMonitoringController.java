package com.lorafilm.booking.monitoring.controller;

import com.lorafilm.booking.booking.repository.BookingRepository;
import com.lorafilm.booking.common.response.ApiResponse;
import com.lorafilm.booking.infrastructure.repository.BookingRetryTaskRepository;
import com.lorafilm.booking.infrastructure.repository.BookingReconciliationTaskRepository;
import com.lorafilm.booking.infrastructure.enums.ReconciliationStatus;
import com.lorafilm.booking.monitoring.dto.MonitoringSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS_MANAGER', 'FINANCE_DIRECTOR')")
@Tag(name = "Admin Monitoring API", description = "Admin endpoints for monitoring service health and metrics summary")
public class AdminMonitoringController {

    private final BookingRepository bookingRepository;
    private final BookingRetryTaskRepository retryTaskRepository;
    private final BookingReconciliationTaskRepository reconciliationTaskRepository;

    private volatile CachedSummary cachedSummary = null;

    public AdminMonitoringController(
            BookingRepository bookingRepository,
            BookingRetryTaskRepository retryTaskRepository,
            BookingReconciliationTaskRepository reconciliationTaskRepository) {
        this.bookingRepository = bookingRepository;
        this.retryTaskRepository = retryTaskRepository;
        this.reconciliationTaskRepository = reconciliationTaskRepository;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get service monitoring summary", description = "Retrieve comprehensive system metrics summary with 10s TTL caching")
    public ResponseEntity<ApiResponse<MonitoringSummaryResponse>> getMonitoringSummary() {
        CachedSummary cached = getCachedSummary();
        MonitoringSummaryResponse response = new MonitoringSummaryResponse(
                cached.bookingToday,
                cached.paymentFailed,
                cached.expiredBooking,
                cached.pendingRetry,
                cached.promotionReconciliationMismatch
        );
        return ResponseEntity.ok(ApiResponse.success("Monitoring summary retrieved successfully", response));
    }

    private synchronized CachedSummary getCachedSummary() {
        long now = System.currentTimeMillis();
        if (cachedSummary == null || (now - cachedSummary.timestamp) > 10000) {
            java.time.Instant todayStart = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                    .atStartOfDay(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toInstant();
            long bookingToday = bookingRepository.countByCreatedAtAfter(todayStart);
            long paymentFailed = bookingRepository.countByPaymentStatus(com.lorafilm.booking.booking.enums.PaymentStatus.FAILED);
            long expiredBooking = bookingRepository.countByBookingStatus(com.lorafilm.booking.booking.enums.BookingStatus.EXPIRED);
            long pendingRetry = retryTaskRepository.countByStatus(com.lorafilm.booking.infrastructure.enums.RetryTaskStatus.PENDING);
            long promotionReconciliationMismatch = reconciliationTaskRepository
                    .countByReconciliationStatusAndReasonStartingWith(
                            ReconciliationStatus.MISMATCH, "PROMOTION_");
            cachedSummary = new CachedSummary(
                    bookingToday, paymentFailed, expiredBooking, pendingRetry,
                    promotionReconciliationMismatch);
        }
        return cachedSummary;
    }

    private static class CachedSummary {
        final long bookingToday;
        final long paymentFailed;
        final long expiredBooking;
        final long pendingRetry;
        final long promotionReconciliationMismatch;
        final long timestamp;

        CachedSummary(long bookingToday, long paymentFailed, long expiredBooking,
                      long pendingRetry, long promotionReconciliationMismatch) {
            this.bookingToday = bookingToday;
            this.paymentFailed = paymentFailed;
            this.expiredBooking = expiredBooking;
            this.pendingRetry = pendingRetry;
            this.promotionReconciliationMismatch = promotionReconciliationMismatch;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
