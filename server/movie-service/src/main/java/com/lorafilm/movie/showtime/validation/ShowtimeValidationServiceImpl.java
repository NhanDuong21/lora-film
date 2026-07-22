package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ShowtimeValidationServiceImpl implements ShowtimeValidationService {

    private final ShowtimeRepository showtimeRepository;
    private final CinemaClosurePeriodRepository cinemaClosureRepository;
    private final AuditoriumMaintenanceWindowRepository auditoriumMaintenanceRepository;
    private final CinemaOperatingHourRepository cinemaOperatingHourRepository;
    private final MovieShowtimeEligibilityPolicy movieEligibilityPolicy;

    public ShowtimeValidationServiceImpl(ShowtimeRepository showtimeRepository,
                                         CinemaClosurePeriodRepository cinemaClosureRepository,
                                         AuditoriumMaintenanceWindowRepository auditoriumMaintenanceRepository,
                                         CinemaOperatingHourRepository cinemaOperatingHourRepository,
                                         MovieShowtimeEligibilityPolicy movieEligibilityPolicy) {
        this.showtimeRepository = showtimeRepository;
        this.cinemaClosureRepository = cinemaClosureRepository;
        this.auditoriumMaintenanceRepository = auditoriumMaintenanceRepository;
        this.cinemaOperatingHourRepository = cinemaOperatingHourRepository;
        this.movieEligibilityPolicy = movieEligibilityPolicy;
    }

    @Override
    public void validateScheduling(ShowtimeValidationContext context) {
        if (context == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Showtime validation context is required");
        }

        movieEligibilityPolicy.validateMovieAndVersion(context.getMovie(), context.getMovieVersion());
        validateCinemaAndAuditorium(context.getCinema(), context.getAuditorium());
        ZoneId cinemaZone = resolveCinemaZone(context.getCinema());
        validateTimeAndDuration(context);
        movieEligibilityPolicy.validateReleaseWindow(context.getMovie(), context.getStartTime(), cinemaZone);
        validateOperatingHours(context.getStartTime(), context.getEndTime(), context.getCinema(), cinemaZone);
        validateOverlaps(context);
    }

    private void validateCinemaAndAuditorium(Cinema cinema, Auditorium auditorium) {
        if (cinema == null || cinema.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_FOUND);
        }
        if (cinema.getStatus() != com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_ACTIVE, "Cinema must be ACTIVE");
        }
        
        if (auditorium == null || auditorium.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND);
        }

        if (auditorium.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_ACTIVE, "Auditorium must be ACTIVE");
        }
        
        if (auditorium.getCinema() == null
                || auditorium.getCinema().getId() == null
                || !auditorium.getCinema().getId().equals(cinema.getId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA, "Auditorium does not belong to cinema");
        }
    }

    private void validateTimeAndDuration(ShowtimeValidationContext context) {
        if (context.getStartTime() == null || context.getEndTime() == null || !context.getEndTime().isAfter(context.getStartTime())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Showtime end time must be after start time");
        }

        Integer cleaningBuffer = context.getAuditorium().getCleaningBufferMinutes();
        if (cleaningBuffer == null || cleaningBuffer < 0) {
            throw new BusinessException(ErrorCode.INVALID_CLEANING_BUFFER);
        }
    }

    private ZoneId resolveCinemaZone(Cinema cinema) {
        try {
            return ZoneId.of(cinema.getTimezone());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE, "Invalid timezone configured for cinema");
        }
    }

    private void validateOperatingHours(Instant startTime, Instant endTime, Cinema cinema, ZoneId zoneId) {
        ZonedDateTime startZdt = startTime.atZone(zoneId);
        ZonedDateTime endZdt = endTime.atZone(zoneId);
        
        // Java DayOfWeek: 1 = Monday, 7 = Sunday. We assume CinemaOperatingHour maps DayOfWeek 1-7.
        int dayOfWeek = startZdt.getDayOfWeek().getValue();
        int prevDayOfWeek = dayOfWeek == 1 ? 7 : dayOfWeek - 1;
        
        List<CinemaOperatingHour> operatingHours = cinemaOperatingHourRepository.findByCinemaId(cinema.getId());
        CinemaOperatingHour todayOpHour = operatingHours.stream()
                .filter(oh -> oh.getDayOfWeek() != null && oh.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);
        CinemaOperatingHour prevOpHour = operatingHours.stream()
                .filter(oh -> oh.getDayOfWeek() != null && oh.getDayOfWeek() == prevDayOfWeek)
                .findFirst()
                .orElse(null);

        boolean todayValid = checkOperatingWindow(startZdt, endZdt, todayOpHour, startZdt.toLocalDate(), zoneId);
        boolean prevValid = checkOperatingWindow(startZdt, endZdt, prevOpHour, startZdt.toLocalDate().minusDays(1), zoneId);

        if (!todayValid && !prevValid) {
            if (todayOpHour == null) {
                throw new BusinessException(ErrorCode.CINEMA_OPERATING_HOURS_NOT_CONFIGURED, "Cinema operating hours are not configured for the selected day");
            }
            throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Showtime outside operating hours");
        }
    }

    private boolean checkOperatingWindow(ZonedDateTime startZdt,
                                         ZonedDateTime endZdt,
                                         CinemaOperatingHour opHour,
                                         LocalDate operatingDate,
                                         ZoneId zoneId) {
        if (opHour == null || Boolean.TRUE.equals(opHour.getIsClosed()) || opHour.getOpenTime() == null || opHour.getCloseTime() == null) {
            return false;
        }
        
        ZonedDateTime openDateTime = operatingDate.atTime(opHour.getOpenTime()).atZone(zoneId);
        LocalDate closeDate = operatingDate;
        if (opHour.getCloseTime().isBefore(opHour.getOpenTime())) {
            closeDate = closeDate.plusDays(1);
        }
        ZonedDateTime closeDateTime = closeDate.atTime(opHour.getCloseTime()).atZone(zoneId);
        
        return !startZdt.isBefore(openDateTime) && !endZdt.isAfter(closeDateTime);
    }

    private void validateOverlaps(ShowtimeValidationContext context) {
        Instant occupancyEndTime = context.getOccupancyEndTime();

        // 1. Cinema Closure
        List<CinemaClosurePeriod> closures = cinemaClosureRepository.findOverlappingClosures(
                context.getCinema().getId(), context.getStartTime(), occupancyEndTime);
        if (!closures.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE, "Showtime overlaps with cinema closure");
        }

        // 2. Auditorium Maintenance
        boolean hasMaintenance = auditoriumMaintenanceRepository.existsOverlap(
                context.getAuditorium().getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, context.getStartTime(), occupancyEndTime);
        if (hasMaintenance) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE, "Showtime overlaps with auditorium maintenance");
        }

        // 3. Existing Showtimes
        Instant candidateStartMinusBuffer = context.getStartTime().minus(context.getCleaningBufferMinutes(), java.time.temporal.ChronoUnit.MINUTES);

        List<Showtime> overlappingShowtimes;
        if (context.getExcludeShowtimeId().isPresent()) {
            overlappingShowtimes = showtimeRepository.findBlockingOverlapsForScheduling(
                    context.getAuditorium().getId(), candidateStartMinusBuffer, occupancyEndTime, context.getExcludeShowtimeId().get());
        } else {
            overlappingShowtimes = showtimeRepository.findBlockingOverlapsForScheduling(
                    context.getAuditorium().getId(), candidateStartMinusBuffer, occupancyEndTime);
        }

        if (!overlappingShowtimes.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAP_CONFLICT, "Showtime overlaps with an existing showtime");
        }
    }
}
