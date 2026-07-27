package com.lorafilm.booking.booking.dto;

public record BookingOperationsSummaryResponse(
        long totalBookings,
        long pendingPayment,
        long confirmed,
        long completed,
        long cancelled,
        long expired,
        long refunded,
        long expiringSoon,
        long overdue,
        long paymentFailed,
        long needsAttention) {
}
