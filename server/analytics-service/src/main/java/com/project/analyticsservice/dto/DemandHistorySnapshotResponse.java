package com.project.analyticsservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DemandHistorySnapshotResponse(
        String snapshotVersion,
        Instant generatedAt,
        LocalDate historyFrom,
        LocalDate historyTo,
        long sourceBookingFactCount,
        long factsWithShowtimeContext,
        Aggregate cinemaPrior,
        List<MovieHistory> movies,
        List<SlotHistory> slots,
        List<FormatHistory> formats) {

    public record Aggregate(
            long bookingCount,
            long showtimeCount,
            long ticketCount,
            BigDecimal averageOccupancy,
            BigDecimal ticketsPerDay,
            BigDecimal recentTicketsPerDay,
            BigDecimal previousTicketsPerDay,
            BigDecimal refundRate,
            BigDecimal cancellationRate,
            BigDecimal averageNetRevenuePerTicket,
            boolean hasShowtimeContext) {
    }

    public record MovieHistory(String moviePublicId, Aggregate aggregate) {
    }

    public record SlotHistory(boolean weekend, int hourBucket, Aggregate aggregate) {
    }

    public record FormatHistory(String format, Aggregate aggregate) {
    }
}
