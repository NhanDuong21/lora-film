package com.lorafilm.booking.booking.dto.response;

import java.util.List;

public record EmergencyShowtimeClosureResponse(
        String showtimePublicId,
        int releasedUnlinkedSeatCount,
        List<String> cancelledPendingBookingPublicIds,
        List<EmergencyPaidBookingResponse> cancelledPendingBookings,
        List<EmergencyPaidBookingResponse> paidBookings
) {
    public int cancelledPendingBookingCount() {
        return cancelledPendingBookingPublicIds == null
                ? 0
                : cancelledPendingBookingPublicIds.size();
    }
}
