package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreflightRequest;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.autoschedule.service.MovieFormatCompatibilityPolicy;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreflightServiceImplTest {

    @Mock CinemaRepository cinemaRepository;
    @Mock AuditoriumRepository auditoriumRepository;
    @Mock MovieVersionRepository movieVersionRepository;
    @Mock CinemaOperatingHourRepository operatingHourRepository;
    @Mock CinemaClosurePeriodRepository closureRepository;
    @Mock AuditoriumMaintenanceWindowRepository maintenanceRepository;
    @Mock ShowtimeRepository showtimeRepository;
    @Mock CinemaOperatingWindowResolver operatingWindowResolver;
    @Mock MovieFormatCompatibilityPolicy compatibilityPolicy;
    @Mock PricePolicyResolver pricePolicyResolver;

    private AutoSchedulePreflightServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AutoSchedulePreflightServiceImpl(
                cinemaRepository, auditoriumRepository, movieVersionRepository,
                operatingHourRepository, closureRepository, maintenanceRepository,
                showtimeRepository, operatingWindowResolver, compatibilityPolicy,
                pricePolicyResolver,
                Clock.fixed(Instant.parse("2026-08-06T12:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void rejectsUnsupportedPlanningHorizonBeforeLoadingAnyFacts() {
        AutoSchedulePreflightRequest request = request(2);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.prepare(request));

        assertEquals(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE, exception.getErrorCode());
        verifyNoInteractions(cinemaRepository, auditoriumRepository, movieVersionRepository);
    }

    @Test
    void reportsEmptyEligibilityAndMissingHoursWithoutGeneratingCandidates() {
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-1");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        when(cinemaRepository.findByPublicIdAndDeletedAtIsNull("cinema-1"))
                .thenReturn(Optional.of(cinema));
        when(movieVersionRepository.findEligibleForAutoSchedule(any(), anyList(), any(), any()))
                .thenReturn(List.of());
        when(auditoriumRepository.findByCinemaIdAndStatusAndDeletedAtIsNull(any(), any()))
                .thenReturn(List.of());
        when(operatingHourRepository.findByCinemaId(1L)).thenReturn(List.of());
        when(operatingWindowResolver.resolve(any(Cinema.class), any(LocalDate.class),
                any(LocalDate.class), anyList())).thenReturn(List.of());

        var result = service.prepare(request(1));
        List<String> codes = result.response().blockers().stream().map(blocker -> blocker.code()).toList();

        assertFalse(result.response().canGenerate());
        assertTrue(codes.contains("NO_ELIGIBLE_VERSIONS"));
        assertTrue(codes.contains("NO_ELIGIBLE_AUDITORIUMS"));
        assertTrue(codes.contains("MISSING_OPERATING_HOURS"));
        assertEquals(0, result.response().compatiblePairCount());
    }

    private AutoSchedulePreflightRequest request(int days) {
        AutoSchedulePreflightRequest request = new AutoSchedulePreflightRequest();
        request.setCinemaPublicId("cinema-1");
        request.setPlanningDays(days);
        return request;
    }
}
