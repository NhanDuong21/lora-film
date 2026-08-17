package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShowtimeFacilityAvailabilityPolicyTest {

    private final CinemaClosurePeriodRepository closureRepository =
            mock(CinemaClosurePeriodRepository.class);
    private final AuditoriumMaintenanceWindowRepository maintenanceRepository =
            mock(AuditoriumMaintenanceWindowRepository.class);
    private final ShowtimeFacilityAvailabilityPolicy policy =
            new ShowtimeFacilityAvailabilityPolicy(closureRepository, maintenanceRepository);

    private Showtime showtime;

    @BeforeEach
    void setUp() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setStatus(CinemaStatus.ACTIVE);

        Auditorium auditorium = new Auditorium();
        auditorium.setId(2L);
        auditorium.setCinema(cinema);
        auditorium.setStatus(AuditoriumStatus.ACTIVE);

        showtime = new Showtime();
        showtime.setPublicId("showtime-1");
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.parse("2026-08-20T10:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-08-20T12:00:00Z"));
    }

    @Test
    void allowsSaleWhenLifecycleAndWindowsAreAvailable() {
        when(closureRepository.findOverlappingClosures(
                1L, showtime.getStartTime(), showtime.getEndTime())).thenReturn(List.of());
        when(maintenanceRepository.existsOverlap(
                2L, ActionStatus.ACTIVE, showtime.getStartTime(), showtime.getEndTime()))
                .thenReturn(false);

        assertThatCode(() -> policy.validateAvailable(showtime)).doesNotThrowAnyException();
    }

    @Test
    void rejectsSaleWhenCinemaIsNotActive() {
        showtime.getCinema().setStatus(CinemaStatus.TEMPORARILY_CLOSED);

        assertUnavailable();
    }

    @Test
    void rejectsSaleWhenClosureOverlapsShowtime() {
        when(closureRepository.findOverlappingClosures(
                1L, showtime.getStartTime(), showtime.getEndTime()))
                .thenReturn(List.of(new CinemaClosurePeriod()));

        assertUnavailable();
    }

    @Test
    void rejectsSaleWhenMaintenanceOverlapsShowtime() {
        when(closureRepository.findOverlappingClosures(
                1L, showtime.getStartTime(), showtime.getEndTime())).thenReturn(List.of());
        when(maintenanceRepository.existsOverlap(
                2L, ActionStatus.ACTIVE, showtime.getStartTime(), showtime.getEndTime()))
                .thenReturn(true);

        assertUnavailable();
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> policy.validateAvailable(showtime))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHOWTIME_FACILITY_UNAVAILABLE);
    }
}
