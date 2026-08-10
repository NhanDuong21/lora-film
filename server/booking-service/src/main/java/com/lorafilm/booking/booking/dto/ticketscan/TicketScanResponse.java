package com.lorafilm.booking.booking.dto.ticketscan;

import com.lorafilm.booking.booking.enums.TicketScanResult;
import java.time.Instant;

public record TicketScanResponse(
        String eventPublicId,
        TicketScanResult result,
        boolean admitted,
        String reasonCode,
        String message,
        String ticketPublicId,
        String ticketCode,
        String bookingPublicId,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String auditoriumName,
        String seatLabel,
        Instant showtimeStart,
        Instant showtimeEnd,
        Instant usedAt,
        Long employeeAccountId,
        String gateLabel,
        Instant scannedAt) {
}
