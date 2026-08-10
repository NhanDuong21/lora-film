package com.lorafilm.booking.booking.dto.ticketscan;

import java.time.Instant;

public record TicketShowtimeOperationResponse(
        String showtimePublicId,
        String movieTitle,
        String auditoriumName,
        Instant showtimeStart,
        Instant showtimeEnd,
        int totalTickets,
        int admittedTickets,
        int remainingTickets,
        String entryStatus) {
}
