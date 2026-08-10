package com.lorafilm.movie.autoschedule.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DemandHistorySnapshot(
        boolean sourceAvailable,
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

    public static DemandHistorySnapshot unavailable(LocalDate from, LocalDate to, Instant now) {
        return new DemandHistorySnapshot(false, "ANALYTICS_UNAVAILABLE", now, from, to,
                0, 0, Aggregate.empty(), List.of(), List.of(), List.of());
    }

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
        public static Aggregate empty() {
            return new Aggregate(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, false);
        }
    }

    public record MovieHistory(String moviePublicId, Aggregate aggregate) {
    }

    public record SlotHistory(boolean weekend, int hourBucket, Aggregate aggregate) {
    }

    public record FormatHistory(String format, Aggregate aggregate) {
    }
}
