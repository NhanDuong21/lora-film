package com.lorafilm.movie.showtime.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerSeatLayoutResponse(
        String showtimePublicId,
        LocalDate serviceDate,
        Instant startTime,
        Instant endTime,
        LocalDateTime localStartTime,
        LocalDateTime localEndTime,
        MovieContext movie,
        VersionContext movieVersion,
        CinemaContext cinema,
        AuditoriumContext auditorium,
        List<CustomerSeat> seats) {

    public record MovieContext(String publicId, String slug, String title) {}
    public record VersionContext(String publicId, String versionName, String format,
                                 String audioLanguage, String subtitleLanguage) {}
    public record CinemaContext(String publicId, String slug, String name, String timezone) {}
    public record AuditoriumContext(String publicId, String name, String screenType, String soundType) {}
    public record CustomerSeat(
            String publicId,
            String seatCode,
            String rowLabel,
            Integer seatNumber,
            Integer positionRow,
            Integer positionColumn,
            String seatType,
            String seatTypeName,
            BigDecimal price,
            String currency,
            String operationalStatus,
            boolean blockedForShowtime,
            boolean priced,
            boolean sellable) {}
}
