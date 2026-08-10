package com.lorafilm.movie.showtime.dto.response;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;

import java.time.Instant;
import java.util.List;

public record ShowtimeSeatControlResponse(
        String showtimePublicId,
        ShowtimeStatus showtimeStatus,
        String movieTitle,
        String cinemaPublicId,
        String cinemaName,
        String cinemaTimezone,
        String auditoriumPublicId,
        String auditoriumName,
        Instant startTime,
        Instant endTime,
        boolean editable,
        String editabilityMessage,
        int blockedSeatCount,
        List<SeatItem> seats
) {
    public record SeatItem(
            String publicId,
            String seatCode,
            String rowLabel,
            Integer seatNumber,
            Integer positionRow,
            Integer positionColumn,
            String seatTypeCode,
            String seatTypeName,
            String pairGroup,
            String operationalStatus,
            boolean blocked,
            String blockReason,
            Instant blockedAt,
            Long blockedBy
    ) {
    }
}
