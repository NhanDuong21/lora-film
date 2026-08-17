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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Revalidates facility availability at the point of sale.
 *
 * <p>A showtime can remain OPEN_FOR_BOOKING after a closure or maintenance window
 * is created. This policy deliberately does not trust the showtime status alone:
 * every customer-facing sale path must also pass the current facility facts.</p>
 */
@Component
public class ShowtimeFacilityAvailabilityPolicy {

    private final CinemaClosurePeriodRepository cinemaClosureRepository;
    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;

    public ShowtimeFacilityAvailabilityPolicy(
            CinemaClosurePeriodRepository cinemaClosureRepository,
            AuditoriumMaintenanceWindowRepository maintenanceRepository) {
        this.cinemaClosureRepository = cinemaClosureRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    public void validateAvailable(Showtime showtime) {
        if (showtime == null || showtime.getStartTime() == null || showtime.getEndTime() == null) {
            throw unavailable(showtime, "Showtime facility facts are incomplete");
        }

        Cinema cinema = showtime.getCinema();
        if (cinema == null || cinema.getDeletedAt() != null || cinema.getStatus() != CinemaStatus.ACTIVE) {
            throw unavailable(showtime, "Cinema is not active");
        }

        Auditorium auditorium = showtime.getAuditorium();
        if (auditorium == null || auditorium.getDeletedAt() != null
                || auditorium.getStatus() != AuditoriumStatus.ACTIVE
                || auditorium.getCinema() == null
                || !cinema.getId().equals(auditorium.getCinema().getId())) {
            throw unavailable(showtime, "Auditorium is not active in this cinema");
        }

        List<CinemaClosurePeriod> closures = cinemaClosureRepository.findOverlappingClosures(
                cinema.getId(), showtime.getStartTime(), showtime.getEndTime());
        if (closures != null && !closures.isEmpty()) {
            throw unavailable(showtime, "Cinema closure overlaps this showtime");
        }

        boolean maintenanceOverlap = maintenanceRepository.existsOverlap(
                auditorium.getId(), ActionStatus.ACTIVE,
                showtime.getStartTime(), showtime.getEndTime());
        if (maintenanceOverlap) {
            throw unavailable(showtime, "Auditorium maintenance overlaps this showtime");
        }
    }

    public boolean isAvailable(Showtime showtime) {
        try {
            validateAvailable(showtime);
            return true;
        } catch (BusinessException exception) {
            return false;
        }
    }

    private BusinessException unavailable(Showtime showtime, String reason) {
        return new BusinessException(
                ErrorCode.SHOWTIME_FACILITY_UNAVAILABLE,
                reason,
                Map.of("showtimePublicId",
                        showtime == null || showtime.getPublicId() == null
                                ? "unknown" : showtime.getPublicId(),
                        "reason", reason));
    }
}
