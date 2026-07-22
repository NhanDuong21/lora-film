package com.lorafilm.movie.showtime.validation;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
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
    @Spy
    private MovieShowtimeEligibilityPolicy movieEligibilityPolicy = new MovieShowtimeEligibilityPolicy();

    @InjectMocks
    private ShowtimeValidationServiceImpl showtimeValidationService;

    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;
    private Auditorium auditorium;
    private Instant startTime;
    private Instant endTime;
    private ShowtimeValidationContext context;
    private CinemaOperatingHour validOpHour;

    @BeforeEach
    void setUp() {
        movie = new Movie();
        movie.setId(1L);
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setReleaseDate(LocalDate.now().minusDays(5));
        movie.setEndDate(LocalDate.now().plusDays(30));
        movie.setDurationMinutes(120);

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
        auditorium.setCleaningBufferMinutes(15);

        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime startZdt = ZonedDateTime.now(zoneId).withHour(10).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime endZdt = startZdt.plusHours(2);

        startTime = startZdt.toInstant();

        validOpHour = new CinemaOperatingHour();
        validOpHour.setDayOfWeek(startZdt.getDayOfWeek().getValue());
        validOpHour.setOpenTime(LocalTime.of(8, 0));
        validOpHour.setCloseTime(LocalTime.of(23, 0));
        validOpHour.setIsClosed(false);

        endTime = endZdt.toInstant();

        context = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium, startTime,
                startTime.plusSeconds(1800), null);
    }

    @Test
    void validateForScheduling_validContext_shouldPass() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(Collections.emptyList());

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
    void validateForScheduling_invalidCurrentCinemaTimezoneShouldThrowSpecificError() {
        cinema.setTimezone("not-a-zone");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));

        assertEquals(ErrorCode.INVALID_CINEMA_TIMEZONE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_auditoriumNotActive_shouldThrowException() {
        auditorium.setStatus(AuditoriumStatus.INACTIVE);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.AUDITORIUM_NOT_ACTIVE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_invalidDuration_shouldThrowException() {
        Integer[] invalidDurations = { null, 0, -1 };
        for (Integer dur : invalidDurations) {
            movie.setDurationMinutes(dur);
            ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium,
                    startTime, startTime.plusSeconds(1800), null);
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> showtimeValidationService.validateScheduling(ctx));
            assertEquals(ErrorCode.INVALID_MOVIE_DURATION, ex.getErrorCode());
        }
    }

    @Test
    void validateForScheduling_validDuration_shouldPass() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(Collections.emptyList());

        Integer[] validDurations = { 1, 29, 30, 138 };
        for (Integer dur : validDurations) {
            movie.setDurationMinutes(dur);
            ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium,
                    startTime, startTime.plusSeconds(1800), null);
            assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
        }
    }

    @Test
    void validateForScheduling_missingOperatingHours_shouldThrowException() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(Collections.emptyList());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.CINEMA_OPERATING_HOURS_NOT_CONFIGURED, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_outsideOperatingHours_shouldThrowException() {
        validOpHour.setOpenTime(LocalTime.of(12, 0));
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_overnightOperatingHours_shouldPass() {
        // Monday 08:00 to 02:00
        CinemaOperatingHour mondayOpHour = new CinemaOperatingHour();
        mondayOpHour.setDayOfWeek(1); // Monday
        mondayOpHour.setOpenTime(LocalTime.of(8, 0));
        mondayOpHour.setCloseTime(LocalTime.of(2, 0));
        mondayOpHour.setIsClosed(false);

        // Showtime on Tuesday 01:00 AM
        ZonedDateTime startZdt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.TUESDAY))
                .withHour(1).withMinute(0).withSecond(0).withNano(0);

        movie.setDurationMinutes(30); // Ends at 01:30 AM
        ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium,
                startZdt.toInstant(), startZdt.toInstant().plusSeconds(1800), null);

        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(mondayOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
    }

    @Test
    void validateForScheduling_constructsOperatingWindowAcrossNewYorkDstGap() {
        ZoneId zone = ZoneId.of("America/New_York");
        LocalDate date = LocalDate.of(2026, 3, 8);
        cinema.setTimezone(zone.getId());
        movie.setReleaseDate(date.minusDays(1));
        movie.setEndDate(date.plusDays(1));
        movie.setDurationMinutes(60);
        ZonedDateTime localStart = date.atTime(1, 30).atZone(zone);
        ZonedDateTime localEnd = date.atTime(3, 30).atZone(zone);

        CinemaOperatingHour hours = new CinemaOperatingHour();
        hours.setDayOfWeek(date.getDayOfWeek().getValue());
        hours.setOpenTime(LocalTime.MIDNIGHT);
        hours.setCloseTime(LocalTime.of(4, 0));
        hours.setIsClosed(false);

        ShowtimeValidationContext ctx = new ShowtimeValidationContext(
                movie, movieVersion, cinema, auditorium,
                localStart.toInstant(), localEnd.toInstant(), null);
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(hours));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
        assertEquals(60, java.time.Duration.between(ctx.getStartTime(), ctx.getEndTime()).toMinutes());
    }

    @Test
    void validateForScheduling_filmMayEndAtClosingWhileCleaningContinuesAfterClosing() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate date = LocalDate.now(zone).plusDays(1);
        ZonedDateTime localStart = date.atTime(21, 30).atZone(zone);
        Instant filmEnd = date.atTime(23, 0).atZone(zone).toInstant();
        movie.setReleaseDate(date.minusDays(1));
        movie.setEndDate(date.plusDays(1));
        movie.setDurationMinutes(90);

        CinemaOperatingHour hours = new CinemaOperatingHour();
        hours.setDayOfWeek(date.getDayOfWeek().getValue());
        hours.setOpenTime(LocalTime.of(8, 0));
        hours.setCloseTime(LocalTime.of(23, 0));
        hours.setIsClosed(false);

        ShowtimeValidationContext ctx = new ShowtimeValidationContext(
                movie, movieVersion, cinema, auditorium, localStart.toInstant(), filmEnd, null);
        Instant occupancyEnd = filmEnd.plusSeconds(15 * 60L);

        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(hours));
        when(cinemaClosureRepository.findOverlappingClosures(cinema.getId(), ctx.getStartTime(), occupancyEnd))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(
                auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE,
                ctx.getStartTime(), occupancyEnd)).thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(
                auditorium.getId(), ctx.getStartTime().minusSeconds(15 * 60L), occupancyEnd))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(ctx));
    }

    @Test
    void validateForScheduling_overnightOperatingHours_shouldReject_0200_to_0300() {
        // Monday 08:00 to 02:00 (ngày hôm trước)
        CinemaOperatingHour mondayOpHour = new CinemaOperatingHour();
        mondayOpHour.setDayOfWeek(1); // Monday
        mondayOpHour.setOpenTime(LocalTime.of(8, 0));
        mondayOpHour.setCloseTime(LocalTime.of(2, 0));
        mondayOpHour.setIsClosed(false);

        // Cần add thêm cấu hình Thứ Ba (ngày hôm sau) để hệ thống nhận diện được rạp ĐÃ
        // cấu hình lịch cho ngày này
        CinemaOperatingHour tuesdayOpHour = new CinemaOperatingHour();
        tuesdayOpHour.setDayOfWeek(2); // Tuesday
        tuesdayOpHour.setOpenTime(LocalTime.of(8, 0));
        tuesdayOpHour.setCloseTime(LocalTime.of(2, 0));
        tuesdayOpHour.setIsClosed(false);

        // Showtime vào Tuesday 02:00 AM (Đã hết ca overnight của Thứ Hai, và chưa tới
        // giờ mở cửa Thứ Ba)
        ZonedDateTime startZdt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.TUESDAY))
                .withHour(2).withMinute(0).withSecond(0).withNano(0);

        movie.setDurationMinutes(60); // Ends at 03:00 AM
        ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium,
                startZdt.toInstant(), startZdt.toInstant().plusSeconds(3600), null);

        // TRẢ VỀ CẢ 2 NGÀY cấu hình
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId()))
                .thenReturn(List.of(mondayOpHour, tuesdayOpHour));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(ctx));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_overnightOperatingHours_shouldReject_0730_to_0900() {
        // Monday 08:00 to 02:00
        CinemaOperatingHour mondayOpHour = new CinemaOperatingHour();
        mondayOpHour.setDayOfWeek(1);
        mondayOpHour.setOpenTime(LocalTime.of(8, 0));
        mondayOpHour.setCloseTime(LocalTime.of(2, 0));
        mondayOpHour.setIsClosed(false);

        // Thêm cấu hình Thứ Ba
        CinemaOperatingHour tuesdayOpHour = new CinemaOperatingHour();
        tuesdayOpHour.setDayOfWeek(2); // Tuesday
        tuesdayOpHour.setOpenTime(LocalTime.of(8, 0));
        tuesdayOpHour.setCloseTime(LocalTime.of(2, 0));
        tuesdayOpHour.setIsClosed(false);

        // Showtime vào Tuesday 07:30 AM (Vẫn nằm ngoài khung giờ hoạt động)
        ZonedDateTime startZdt = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.TUESDAY))
                .withHour(7).withMinute(30).withSecond(0).withNano(0);

        movie.setDurationMinutes(90); // Ends at 09:00 AM
        ShowtimeValidationContext ctx = new ShowtimeValidationContext(movie, movieVersion, cinema, auditorium,
                startZdt.toInstant(), startZdt.toInstant().plusSeconds(5400), null);

        // TRẢ VỀ CẢ 2 NGÀY cấu hình
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId()))
                .thenReturn(List.of(mondayOpHour, tuesdayOpHour));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(ctx));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_OPERATING_HOURS, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_overlapsWithOtherShowtime_shouldThrowException() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);

        Showtime overlapping = new Showtime();
        overlapping.setId(10L);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(List.of(overlapping));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));
        assertEquals(ErrorCode.SHOWTIME_OVERLAP_CONFLICT, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_releaseStartUsesCinemaLocalMidnight() {
        LocalDate releaseDate = LocalDate.of(2026, 7, 22);
        movie.setReleaseDate(releaseDate);
        movie.setEndDate(releaseDate.plusDays(10));

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        Instant beforeLocalMidnight = releaseDate.atStartOfDay(zone).minusMinutes(1).toInstant();
        ShowtimeValidationContext beforeContext = new ShowtimeValidationContext(
                movie, movieVersion, cinema, auditorium,
                beforeLocalMidnight, beforeLocalMidnight.plusSeconds(1800), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(beforeContext));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_finalEndDateStartMayFinishAfterEndExclusive() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate finalDate = LocalDate.of(2026, 7, 22);
        movie.setReleaseDate(finalDate.minusDays(10));
        movie.setEndDate(finalDate);
        movie.setDurationMinutes(30);

        ZonedDateTime localStart = finalDate.atTime(23, 50).atZone(zone);
        Instant candidateStart = localStart.toInstant();
        Instant filmEnd = localStart.plusMinutes(30).toInstant();

        CinemaOperatingHour overnight = new CinemaOperatingHour();
        overnight.setDayOfWeek(finalDate.getDayOfWeek().getValue());
        overnight.setOpenTime(LocalTime.of(8, 0));
        overnight.setCloseTime(LocalTime.of(2, 0));
        overnight.setIsClosed(false);

        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(overnight));
        when(cinemaClosureRepository.findOverlappingClosures(eq(cinema.getId()), any(), any()))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(eq(auditorium.getId()), any(), any(), any()))
                .thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), any()))
                .thenReturn(Collections.emptyList());

        ShowtimeValidationContext finalDayContext = new ShowtimeValidationContext(
                movie, movieVersion, cinema, auditorium, candidateStart, filmEnd, null);

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(finalDayContext));
        org.junit.jupiter.api.Assertions.assertTrue(filmEnd.isAfter(finalDate.plusDays(1).atStartOfDay(zone).toInstant()));
    }

    @Test
    void validateForScheduling_startAtEndExclusiveIsRejected() {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate finalDate = LocalDate.of(2026, 7, 22);
        movie.setReleaseDate(finalDate.minusDays(10));
        movie.setEndDate(finalDate);

        Instant endExclusive = finalDate.plusDays(1).atStartOfDay(zone).toInstant();
        ShowtimeValidationContext endBoundaryContext = new ShowtimeValidationContext(
                movie, movieVersion, cinema, auditorium,
                endExclusive, endExclusive.plusSeconds(1800), null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(endBoundaryContext));
        assertEquals(ErrorCode.SHOWTIME_OUTSIDE_RELEASE_WINDOW, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_closureAndMaintenanceReceiveOccupancyEnd() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        Instant occupancyEnd = context.getEndTime().plus(15, java.time.temporal.ChronoUnit.MINUTES);
        when(cinemaClosureRepository.findOverlappingClosures(cinema.getId(), context.getStartTime(), occupancyEnd))
                .thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(
                auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE,
                context.getStartTime(), occupancyEnd)).thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(eq(auditorium.getId()), any(), eq(occupancyEnd)))
                .thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(context));

        verify(cinemaClosureRepository).findOverlappingClosures(
                cinema.getId(), context.getStartTime(), occupancyEnd);
        verify(auditoriumMaintenanceRepository).existsOverlap(
                auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE,
                context.getStartTime(), occupancyEnd);
    }

    @Test
    void validateForScheduling_maintenanceDuringCleaningOccupancyIsRejected() {
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        Instant occupancyEnd = context.getOccupancyEndTime();
        when(cinemaClosureRepository.findOverlappingClosures(
                cinema.getId(), context.getStartTime(), occupancyEnd)).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(
                auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE,
                context.getStartTime(), occupancyEnd)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> showtimeValidationService.validateScheduling(context));

        assertEquals(ErrorCode.SHOWTIME_OVERLAPS_AUDITORIUM_MAINTENANCE, ex.getErrorCode());
    }

    @Test
    void validateForScheduling_zeroBufferUsesFilmEndAsOccupancyEnd() {
        auditorium.setCleaningBufferMinutes(0);
        when(cinemaOperatingHourRepository.findByCinemaId(cinema.getId())).thenReturn(List.of(validOpHour));
        when(cinemaClosureRepository.findOverlappingClosures(
                cinema.getId(), context.getStartTime(), context.getEndTime())).thenReturn(Collections.emptyList());
        when(auditoriumMaintenanceRepository.existsOverlap(
                auditorium.getId(), com.lorafilm.movie.common.enums.ActionStatus.ACTIVE,
                context.getStartTime(), context.getEndTime())).thenReturn(false);
        when(showtimeRepository.findBlockingOverlapsForScheduling(
                auditorium.getId(), context.getStartTime(), context.getEndTime())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> showtimeValidationService.validateScheduling(context));
    }
}
