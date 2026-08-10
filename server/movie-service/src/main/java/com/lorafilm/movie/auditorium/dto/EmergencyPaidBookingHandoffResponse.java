package com.lorafilm.movie.auditorium.dto;

import java.math.BigDecimal;
import java.util.List;

public record EmergencyPaidBookingHandoffResponse(
        String showtimePublicId,
        String bookingPublicId,
        String bookingCode,
        Long userId,
        String bookingStatus,
        BigDecimal finalAmount,
        String currency,
        List<String> seatLabels
) {
}
