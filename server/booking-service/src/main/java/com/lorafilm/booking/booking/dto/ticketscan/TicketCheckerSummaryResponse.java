package com.lorafilm.booking.booking.dto.ticketscan;

import java.time.LocalDate;
import java.util.List;

public record TicketCheckerSummaryResponse(
        LocalDate date,
        long totalScans,
        long admittedScans,
        long rejectedScans,
        long duplicateScans,
        long totalTickets,
        long admittedTickets,
        long remainingTickets,
        List<TicketShowtimeOperationResponse> showtimes,
        TicketGateHandoffResponse handoff) {
}
