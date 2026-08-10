package com.lorafilm.movie.auditorium.dto;

import java.util.List;

public record EmergencyMaintenanceSummaryResponse(
        int closedShowtimeCount,
        int releasedSeatHoldCount,
        int cancelledPendingBookingCount,
        int stoppedPaymentAttemptCount,
        boolean processingComplete,
        List<EmergencyPaidBookingHandoffResponse> paidBookings,
        List<String> warnings
) {
    public static EmergencyMaintenanceSummaryResponse empty() {
        return new EmergencyMaintenanceSummaryResponse(
                0, 0, 0, 0, true, List.of(), List.of());
    }
}
