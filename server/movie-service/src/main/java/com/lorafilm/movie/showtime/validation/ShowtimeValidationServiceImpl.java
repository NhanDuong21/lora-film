package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class ShowtimeValidationServiceImpl implements ShowtimeValidationService {

    private final ShowtimeRepository showtimeRepository;
    private final CinemaClosurePeriodRepository cinemaClosureRepository;
    private final AuditoriumMaintenanceWindowRepository auditoriumMaintenanceRepository;
    private final CinemaOperatingHourRepository cinemaOperatingHourRepository;

    public ShowtimeValidationServiceImpl(ShowtimeRepository showtimeRepository,
                                         CinemaClosurePeriodRepository cinemaClosureRepository,
                                         AuditoriumMaintenanceWindowRepository auditoriumMaintenanceRepository,
                                         CinemaOperatingHourRepository cinemaOperatingHourRepository) {
        this.showtimeRepository = showtimeRepository;
        this.cinemaClosureRepository = cinemaClosureRepository;
        this.auditoriumMaintenanceRepository = auditoriumMaintenanceRepository;
        this.cinemaOperatingHourRepository = cinemaOperatingHourRepository;
    }

    @Override
    public void validateScheduling(ShowtimeValidationContext context) {
        validateMovie(context.getMovie(), context.getMovieVersion(), context.getStartTime());
        validateCinemaAndAuditorium(context.getCinema(), context.getAuditorium());
        validateTimeAndDuration(context.getStartTime(), context.getEndTime(), context.getCinema(), context.getMovie());
        validateOverlaps(context);
    }

    private void validateMovie(Movie movie, MovieVersion version, Instant startTime) {
        if (movie.getStatus() != com.lorafilm.movie.movie.domain.enums.MovieStatus.NOW_SHOWING && 
            movie.getStatus() != com.lorafilm.movie.movie.domain.enums.MovieStatus.UPCOMING) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING, "Movie must be NOW_SHOWING or UPCOMING");
        }
        
        if (version.getStatus() != ActiveStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_ACTIVE, "Movie version must be ACTIVE");
        }
        
        if (!version.getMovie().getId().equals(movie.getId())) {
            throw new BusinessException(ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE, "Movie version does not belong to the movie");
        }

        if (movie.getReleaseDate() != null) {
            Instant releaseInstant = movie.getReleaseDate().atStartOfDay(ZoneId.of("UTC")).toInstant();
            if (startTime.isBefore(releaseInstant)) {
                throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW, "Showtime cannot be scheduled before movie release date");
            }
        }
        
        if (movie.getEndDate() != null) {
            Instant endInstant = movie.getEndDate().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
            if (startTime.isAfter(endInstant)) {
                throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW, "Showtime cannot be scheduled after movie end date");
            }
        }
    }

    private void validateCinemaAndAuditorium(Cinema cinema, Auditorium auditorium) {
        if (cinema.getStatus() != com.lorafilm.movie.cinema.domain.enums.CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.CINEMA_NOT_ACTIVE, "Cinema must be ACTIVE");
        }
        
        if (auditorium.getStatus() != com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_ACTIVE, "Auditorium must be ACTIVE");
        }
        
        if (!auditorium.getCinema().getId().equals(cinema.getId())) {
            throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA, "Auditorium does not belong to cinema");
        }
    }

    private void validateTimeAndDuration(Instant startTime, Instant endTime, Cinema cinema, Movie movie) {
        Duration duration = Duration.between(startTime, endTime);
        if (duration.toMinutes() < 30) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Showtime duration must be at least 30 minutes");
        }
        
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(cinema.getTimezone());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE, "Invalid timezone configured for cinema");
        }

        // Operating hours check
        ZonedDateTime startZdt = startTime.atZone(zoneId);
        ZonedDateTime endZdt = endTime.atZone(zoneId);
        
        // Java DayOfWeek: 1 = Monday, 7 = Sunday. We assume CinemaOperatingHour maps DayOfWeek 1-7.
        int dayOfWeek = startZdt.getDayOfWeek().getValue();
        
        List<CinemaOperatingHour> operatingHours = cinemaOperatingHourRepository.findByCinemaId(cinema.getId());
        CinemaOperatingHour todayOpHour = operatingHours.stream()
                .filter(oh -> oh.getDayOfWeek() != null && oh.getDayOfWeek() == dayOfWeek)
                .findFirst()
                .orElse(null);

        if (todayOpHour == null) {
            // No configuration means we probably don't restrict, or we restrict completely.
            // But let's assume default 08:00 - 23:59 if missing, as per requirement hint.
            LocalTime defaultOpen = LocalTime.of(8, 0);
            LocalTime defaultClose = LocalTime.of(23, 59);
            validateAgainstOperatingHours(startZdt.toLocalTime(), endZdt.toLocalTime(), defaultOpen, defaultClose, false);
        } else {
            validateAgainstOperatingHours(startZdt.toLocalTime(), endZdt.toLocalTime(), 
                    todayOpHour.getOpenTime(), todayOpHour.getCloseTime(), todayOpHour.getIsClosed());
        }
    }

    private void validateAgainstOperatingHours(LocalTime start, LocalTime end, LocalTime open, LocalTime close, boolean isClosed) {
        if (isClosed) {
            throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Cinema is closed on this day of week");
        }
        if (open == null || close == null) {
            return; // Config invalid, skip validation or allow
        }
        if (start.isBefore(open) || end.isAfter(close)) {
            // Handle cross-midnight close time, e.g. open 08:00, close 02:00
            if (close.isBefore(open)) {
                boolean isValid = (!start.isBefore(open) || start.isBefore(close)) && 
                                  (!end.isAfter(close) || end.isAfter(open));
                if (!isValid) {
                    throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Showtime outside operating hours");
                }
            } else {
                throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Showtime outside operating hours");
            }
        }
    }

    private void validateOverlaps(ShowtimeValidationContext context) {
        // 1. Cinema Closure
        List<CinemaClosurePeriod> closures = cinemaClosureRepository.findOverlappingClosures(
                context.getCinema().getId(), context.getStartTime(), context.getEndTime());
        if (!closures.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_CINEMA_CLOSURE, "Showtime overlaps with cinema closure");
        }

        // 2. Auditorium Maintenance
        boolean hasMaintenance = auditoriumMaintenanceRepository.existsOverlap(
                context.getAuditorium().getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE, context.getStartTime(), context.getEndTime());
        if (hasMaintenance) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE, "Showtime overlaps with auditorium maintenance");
        }

        // 3. Existing Showtimes
        List<Showtime> overlappingShowtimes;
        if (context.getExcludeShowtimeId().isPresent()) {
            overlappingShowtimes = showtimeRepository.findPotentialOverlaps(
                    context.getAuditorium().getId(), context.getStartTime(), context.getEndTime(), context.getExcludeShowtimeId().get());
        } else {
            overlappingShowtimes = showtimeRepository.findPotentialOverlaps(
                    context.getAuditorium().getId(), context.getStartTime(), context.getEndTime());
        }

        if (!overlappingShowtimes.isEmpty()) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAP, "Showtime overlaps with an existing showtime");
        }
    }
}
