package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.CandidateValidationResult;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.ShowtimeCandidateValidationService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationContext;
import com.lorafilm.movie.showtime.validation.ShowtimeValidationService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ShowtimeCandidateValidationServiceImpl implements ShowtimeCandidateValidationService {

    private final ShowtimeValidationService showtimeValidationService;

    public ShowtimeCandidateValidationServiceImpl(ShowtimeValidationService showtimeValidationService) {
        this.showtimeValidationService = showtimeValidationService;
    }

    @Override
    public CandidateValidationResult validate(ShowtimeCandidate candidate) {
        // Sanity checks for time logic
        if (candidate.getEndTime() == null || !candidate.getEndTime().isAfter(candidate.getStartTime())) {
            return CandidateValidationResult.rejected("INVALID_CANDIDATE_TIME", "End time must be after start time");
        }
        if (candidate.getOccupancyEndTime() == null || candidate.getOccupancyEndTime().isBefore(candidate.getEndTime())) {
            return CandidateValidationResult.rejected("INVALID_CANDIDATE_TIME", "Occupancy end time cannot be before end time");
        }

        // Prepare context for the existing validation service
        ShowtimeValidationContext context = ShowtimeValidationContext.builder()
                .movie(candidate.getMovie())
                .movieVersion(candidate.getMovieVersion())
                .cinema(candidate.getCinema())
                .auditorium(candidate.getAuditorium())
                .startTime(candidate.getStartTime())
                .endTime(candidate.getEndTime())
                .build();

        try {
            showtimeValidationService.validateScheduling(context);
            return CandidateValidationResult.valid();
        } catch (BusinessException e) {
            // Catch the specific business exception and convert to a rejection result
            return CandidateValidationResult.rejected(e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            // Catch any unexpected errors during validation
            return CandidateValidationResult.rejected("UNEXPECTED_VALIDATION_ERROR", e.getMessage());
        }
    }
}
