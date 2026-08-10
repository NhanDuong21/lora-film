package com.lorafilm.booking.booking.dto.ticketscan;

import java.time.Instant;
import java.time.LocalDate;

public record TicketGateHandoffResponse(
        String publicId,
        Long employeeAccountId,
        String cinemaPublicId,
        LocalDate shiftDate,
        String gateLabel,
        int totalScans,
        int successfulScans,
        int rejectedScans,
        int unresolvedIncidents,
        String note,
        Instant handedOffAt) {
}
