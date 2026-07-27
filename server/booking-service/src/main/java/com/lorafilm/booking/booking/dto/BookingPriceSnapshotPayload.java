package com.lorafilm.booking.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingPriceSnapshotPayload(
        Long showtimeId,
        String showtimePublicId,
        Instant capturedAt,
        String currency,
        Long movieId,
        String movieTitle,
        BigDecimal authoritativeTicketTotal,
        List<SeatPriceLine> seats
) {
    public record SeatPriceLine(
            Long seatId,
            String seatLabel,
            String seatType,
            BigDecimal unitPrice,
            String seatPublicId
    ) {
        /** Compatibility constructor for historical snapshot callers. */
        public SeatPriceLine(Long seatId, String seatLabel, String seatType, BigDecimal unitPrice) {
            this(seatId, seatLabel, seatType, unitPrice, null);
        }
    }
}
