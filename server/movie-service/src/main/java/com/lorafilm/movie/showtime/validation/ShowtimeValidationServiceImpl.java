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
        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_DURATION, "Movie duration must be greater than zero");
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

        boolean todayValid = checkOperatingWindow(startZdt, endZdt, todayOpHour, startZdt);
        boolean prevValid = checkOperatingWindow(startZdt, endZdt, prevOpHour, startZdt.minusDays(1));

        if (!todayValid && !prevValid) {
            if (todayOpHour == null) {
                throw new BusinessException(ErrorCode.CINEMA_OPERATING_HOURS_NOT_CONFIGURED, "Cinema operating hours are not configured for the selected day");
            }
            throw new BusinessException(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, "Showtime outside operating hours");
        }
    }

    private boolean checkOperatingWindow(ZonedDateTime startZdt, ZonedDateTime endZdt, CinemaOperatingHour opHour, ZonedDateTime referenceDate) {
        if (opHour == null || Boolean.TRUE.equals(opHour.getIsClosed()) || opHour.getOpenTime() == null || opHour.getCloseTime() == null) {
            return false;
        }
        
        ZonedDateTime openDateTime = referenceDate.withHour(opHour.getOpenTime().getHour())
                                                  .withMinute(opHour.getOpenTime().getMinute())
                                                  .withSecond(0).withNano(0);
        ZonedDateTime closeDateTime;
        if (opHour.getCloseTime().isBefore(opHour.getOpenTime())) {
            closeDateTime = referenceDate.plusDays(1).withHour(opHour.getCloseTime().getHour())
                                                      .withMinute(opHour.getCloseTime().getMinute())
                                                      .withSecond(0).withNano(0);
        } else {
            closeDateTime = referenceDate.withHour(opHour.getCloseTime().getHour())
                                          .withMinute(opHour.getCloseTime().getMinute())
                                          .withSecond(0).withNano(0);
        }
        
        return !startZdt.isBefore(openDateTime) && !endZdt.isAfter(closeDateTime);
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
