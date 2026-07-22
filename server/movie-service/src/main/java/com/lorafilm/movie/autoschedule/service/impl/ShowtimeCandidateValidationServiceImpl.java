package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.showtime.validation.MovieShowtimeEligibilityPolicy;
import com.lorafilm.movie.showtime.validation.ShowtimeSchedulingRules;
import org.springframework.stereotype.Service;

@Service
public class ShowtimeCandidateValidationServiceImpl implements ShowtimeCandidateValidationService {

    private final MovieShowtimeEligibilityPolicy movieEligibilityPolicy;
    private final ShowtimeSchedulingRules schedulingRules;

    public ShowtimeCandidateValidationServiceImpl(MovieShowtimeEligibilityPolicy movieEligibilityPolicy,
                                                  ShowtimeSchedulingRules schedulingRules) {
        this.movieEligibilityPolicy = movieEligibilityPolicy;
        this.schedulingRules = schedulingRules;
    }

    @Override
    public CandidateValidationResult validate(ShowtimeCandidate candidate,
                                              AutoScheduleGenerationContext context) {
        try {
            AutoScheduleGenerationContext.MovieVersionSnapshot version = candidate.getMovieVersionSnapshot();
            AutoScheduleGenerationContext.MovieSnapshot movie = version.movie();
            AutoScheduleGenerationContext.AuditoriumSnapshot auditorium = candidate.getAuditoriumSnapshot();
            AutoScheduleGenerationContext.CinemaSnapshot cinema = context.getCinema();

            MovieShowtimeEligibilityPolicy.MovieFacts movieFacts =
                    new MovieShowtimeEligibilityPolicy.MovieFacts(
                            movie.id(), movie.deleted(), movie.status(), movie.durationMinutes(),
                            movie.releaseDate(), movie.endDate());
            MovieShowtimeEligibilityPolicy.VersionFacts versionFacts =
                    new MovieShowtimeEligibilityPolicy.VersionFacts(
                            false, version.deleted(), version.status(),
                            java.util.Objects.equals(version.movieId(), movie.id()));
            movieEligibilityPolicy.validateMovieAndVersion(movieFacts, versionFacts);

            schedulingRules.validateCinemaAndAuditorium(
                    new ShowtimeSchedulingRules.CinemaFacts(
                            cinema.id(), true, cinema.deleted(), cinema.status()),
                    new ShowtimeSchedulingRules.AuditoriumFacts(
                            auditorium.id(), true, auditorium.deleted(), auditorium.status(), auditorium.cinemaId()));
            schedulingRules.validateTimeAndDuration(
                    candidate.getStartTime(), candidate.getEndTime(), auditorium.cleaningBufferMinutes());
            movieEligibilityPolicy.validateReleaseWindow(movieFacts, candidate.getStartTime(), cinema.zoneId());
            schedulingRules.validateOperatingHours(
                    candidate.getStartTime(), candidate.getEndTime(), cinema.zoneId(),
                    context.getOperatingWindows(), context.getConfiguredOperatingDays());
            schedulingRules.validateNoClosure(
                    context.getCinemaClosures(), candidate.getStartTime(), candidate.getOccupancyEndTime());
            schedulingRules.validateNoMaintenance(
                    context.maintenanceFor(auditorium.id()),
                    candidate.getStartTime(), candidate.getOccupancyEndTime());
            schedulingRules.validateNoShowtimeConflict(
                    context.showtimeConflictsFor(auditorium.id()),
                    candidate.getStartTime(), candidate.getOccupancyEndTime());
            return CandidateValidationResult.valid();
        } catch (BusinessException e) {
            return CandidateValidationResult.rejected(e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            return CandidateValidationResult.rejected("UNEXPECTED_VALIDATION_ERROR", e.getMessage());
        }
    }
}
