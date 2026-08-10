package com.lorafilm.booking.booking.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record EmergencyPaidBookingResponse(
        String bookingPublicId,
        String bookingCode,
        Long userId,
        String bookingStatus,
        BigDecimal finalAmount,
        String currency,
        List<String> seatLabels
) {
}
