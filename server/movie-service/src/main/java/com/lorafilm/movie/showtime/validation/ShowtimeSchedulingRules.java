package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.model.ImmutableIntervalIndex;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Component
public class ShowtimeSchedulingRules {

    public void validateCinemaAndAuditorium(CinemaFacts cinema, AuditoriumFacts auditorium) {
        if (cinema == null || !cinema.present() || cinema.deleted()) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        if (cinema.status() != CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_ACTIVE, "Cinema must be ACTIVE");
        }
        if (auditorium == null || !auditorium.present() || auditorium.deleted()) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND);
        }
        if (auditorium.status() != AuditoriumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_ACTIVE, "Auditorium must be ACTIVE");
        }
        if (auditorium.cinemaId() == null || !auditorium.cinemaId().equals(cinema.id())) {
            throw new BusinessException(
                    ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA, "Auditorium does not belong to cinema");
        }
    }

    public void validateTimeAndDuration(Instant start, Instant end, Integer cleaningBufferMinutes) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Showtime end time must be after start time");
        }
        if (cleaningBufferMinutes == null || cleaningBufferMinutes < 0) {
            throw new BusinessException(ErrorCode.INVALID_CLEANING_BUFFER);
        }
    }

    public void validateOperatingHours(Instant start,
                                       Instant end,
                                       ZoneId zoneId,
                                       List<OperatingWindow> windows,
                                       Set<Integer> configuredDays) {
        boolean contained = windows.stream().anyMatch(window ->
                !start.isBefore(window.getOpenInstant()) && !end.isAfter(window.getCloseInstant()));
        if (contained) {
            return;
        }
        int startDay = start.atZone(zoneId).getDayOfWeek().getValue();
        if (!configuredDays.contains(startDay)) {
            throw new BusinessException(
                    ErrorCode.CINEMA_OPERATING_HOURS_NOT_CONFIGURED,
                    "Cinema operating hours are not configured for the selected day");
        }
        throw new BusinessException(
                ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Showtime outside operating hours");
    }

    public void validateNoClosure(ImmutableIntervalIndex intervals, Instant start, Instant occupancyEnd) {
        validateNoClosure(intervals.overlaps(start, occupancyEnd));
    }

    public void validateNoClosure(boolean overlaps) {
        if (overlaps) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE, "Showtime overlaps with cinema closure");
        }
    }

    public void validateNoMaintenance(ImmutableIntervalIndex intervals, Instant start, Instant occupancyEnd) {
        validateNoMaintenance(intervals.overlaps(start, occupancyEnd));
    }

    public void validateNoMaintenance(boolean overlaps) {
        if (overlaps) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE,
                    "Showtime overlaps with auditorium maintenance");
        }
    }

    public void validateNoShowtimeConflict(ImmutableIntervalIndex intervals, Instant start, Instant occupancyEnd) {
        validateNoShowtimeConflict(intervals.overlaps(start, occupancyEnd));
    }

    public void validateNoShowtimeConflict(boolean overlaps) {
        if (overlaps) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_OVERLAP_CONFLICT, "Showtime overlaps with an existing showtime");
        }
    }

    public record CinemaFacts(Long id, boolean present, boolean deleted, CinemaStatus status) {
    }

    public record AuditoriumFacts(Long id,
                                  boolean present,
                                  boolean deleted,
                                  AuditoriumStatus status,
                                  Long cinemaId) {
    }
}
