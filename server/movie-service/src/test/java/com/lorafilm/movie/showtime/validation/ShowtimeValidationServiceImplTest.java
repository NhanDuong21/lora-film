package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeValidationServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private CinemaClosurePeriodRepository cinemaClosureRepository;
    @Mock
    private AuditoriumMaintenanceWindowRepository auditoriumMaintenanceRepository;
    @Mock
    private CinemaOperatingHourRepository cinemaOperatingHourRepository;

    @InjectMocks
    private ShowtimeValidationServiceImpl showtimeValidationService;

    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;
    private Auditorium auditorium;
    private Instant startTime;
    private Instant endTime;
    private ShowtimeValidationContext context;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setReleaseDate(LocalDate.now().minusDays(5));
        movie.setEndDate(LocalDate.now().plusDays(5));

        movieVersion = new MovieVersion();
        movieVersion.setId(1L);
        movieVersion.setMovie(movie);
        movieVersion.setStatus(ActiveStatus.ACTIVE);

        cinema = new Cinema();
        cinema.setId(1L);
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setTimezone("Asia/Ho_Chi_Minh");

        auditorium = new Auditorium();
        auditorium.setId(1L);
        auditorium.setCinema(cinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);

        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime startZdt = ZonedDateTime.now(zoneId).withHour(10).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endZdt = startZdt.plusHours(2);
        
        startTime = startZdt.toInstant();
        endTime = endZdt.toInstant();

        context = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, endTime, null);
    }

    @Test
    void validateForScheduling_validContext_shouldPass() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any())).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any())).thenReturn(false);
        when(showtimeRepository.findPotentialOverlaps(eq(auditorium.getId()), any(), any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(context));
    }

    @Test
    void validateForScheduling_invalidMovieStatus_shouldThrowException() {
        movie.setStatus(MovieStatus.DRAFT);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_invalidMovieVersionStatus_shouldThrowException() {
        movieVersion.setStatus(ActiveStatus.INACTIVE);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.MOVIE_VERSION_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_movieVersionNotBelongToMovie_shouldThrowException() {
        Movie anotherMovie = new Movie();
        anotherMovie.setId(2L);
        movieVersion.setMovie(anotherMovie);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.MOVIE_VERSION_NOT_BELONG_TO_MOVIE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_outsideReleaseWindow_shouldThrowException() {
        // Set release date in the future
        movie.setReleaseDate(LocalDate.now().plusDays(2));
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_cinemaNotActive_shouldThrowException() {
        cinema.setStatus(CinemaStatus.DRAFT);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.CINEMA_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_auditoriumNotActive_shouldThrowException() {
        auditorium.setStatus(AuditoriumStatus.INACTIVE);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.AUDITORIUM_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_durationTooShort_shouldThrowException() {
        // End time is only 15 minutes after start time
        context = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime, startTime.plusSeconds(15 * 60), null);
        
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.INVALID_MOVIE_DURATION, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_outsideOperatingHours_shouldThrowException() {
        CinemaOperatingHour opHour = new CinemaOperatingHour();
        opHour.setDayOfWeek(startTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek().getValue());
        opHour.setOpenTime(LocalTime.of(12, 0)); // opens at 12
        opHour.setCloseTime(LocalTime.of(22, 0));
        opHour.setIsClosed(false);

        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(opHour));

        // Start time is 10:00, which is before 12:00
        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_overlapsWithOtherShowtime_shouldThrowException() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any())).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any())).thenReturn(false);
        
        Showtime overlapping = new Showtime();
        overlapping.setId(10L);
        when(showtimeRepository.findPotentialOverlaps(eq(auditorium.getId()), any(), any())).thenReturn(List.of(overlapping));

        BusinessException ex = assertThrows(BusinessException.class, 
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OVERLAP, ex.getErrorCode());
    }
}
