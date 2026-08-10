package com.lorafilm.movie.showtime.validation;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.service.MovieService;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ShowtimeOpeningPolicy {
    private final MovieShowtimeEligibilityPolicy movieEligibilityPolicy;
    private final ShowtimeValidationService showtimeValidationService;
    private final ShowtimePricingService showtimePricingService;
    private final MovieService movieService;

    public ShowtimeOpeningPolicy(MovieShowtimeEligibilityPolicy movieEligibilityPolicy,
                                 ShowtimeValidationService showtimeValidationService,
                                 ShowtimePricingService showtimePricingService,
                                 MovieService movieService) {
        this.movieEligibilityPolicy = movieEligibilityPolicy;
        this.showtimeValidationService = showtimeValidationService;
        this.showtimePricingService = showtimePricingService;
        this.movieService = movieService;
    }

    public void validateCanOpen(Showtime showtime, Instant now) {
        if (showtime == null || showtime.getStartTime() == null || !showtime.getStartTime().isAfter(now)) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_CANNOT_OPEN_AFTER_START,
                    "Cannot open showtime for booking after it has started");
        }

        movieEligibilityPolicy.validateMovieCanOpenForBooking(showtime.getMovie());
        movieService.validatePublishConditions(showtime.getMovie().getId());

        ShowtimeValidationContext context = ShowtimeValidationContext.builder()
                .movie(showtime.getMovie())
                .movieVersion(showtime.getMovieVersion())
                .cinema(showtime.getCinema())
                .auditorium(showtime.getAuditorium())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .excludeShowtimeId(showtime.getId())
                .build();
        showtimeValidationService.validateScheduling(context);
        showtimePricingService.validateCompleteness(showtime);
    }

    public Evaluation evaluate(Showtime showtime, Instant now) {
        try {
            validateCanOpen(showtime, now);
            return new Evaluation(true, List.of());
        } catch (BusinessException exception) {
            ErrorCode errorCode = exception.getErrorCode();
            return new Evaluation(false, List.of(new OpeningIssue(
                    errorCode == null ? "OPENING_VALIDATION_FAILED" : errorCode.name(),
                    exception.getMessage())));
        }
    }

    public record Evaluation(boolean openable, List<OpeningIssue> blockers) {}
    public record OpeningIssue(String code, String message) {}
}
