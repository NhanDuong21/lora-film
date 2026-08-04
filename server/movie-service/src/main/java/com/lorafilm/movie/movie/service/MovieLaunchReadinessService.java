package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieLaunchReadinessResponse;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.validation.ShowtimeOpeningPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MovieLaunchReadinessService {
    private static final List<ShowtimeStatus> FUTURE_STATUSES = List.of(
            ShowtimeStatus.DRAFT, ShowtimeStatus.OPEN_FOR_BOOKING);

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieService movieService;
    private final MovieApprovalPolicy approvalPolicy;
    private final ShowtimeOpeningPolicy openingPolicy;
    private final Clock clock;

    public MovieLaunchReadinessService(MovieRepository movieRepository,
                                       ShowtimeRepository showtimeRepository,
                                       MovieService movieService,
                                       MovieApprovalPolicy approvalPolicy,
                                       ShowtimeOpeningPolicy openingPolicy,
                                       Clock clock) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
        this.movieService = movieService;
        this.approvalPolicy = approvalPolicy;
        this.openingPolicy = openingPolicy;
        this.clock = clock;
    }

    public MovieLaunchReadinessResponse get(String moviePublicId) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND));
        Instant now = Instant.now(clock);
        List<Showtime> showtimes = showtimeRepository
                .findByMovieIdAndStatusInAndStartTimeAfterAndDeletedAtIsNullOrderByStartTimeAsc(
                        movie.getId(), FUTURE_STATUSES, now);

        List<MovieLaunchReadinessResponse.LaunchIssue> blockers = new ArrayList<>();
        List<MovieLaunchReadinessResponse.LaunchIssue> warnings = new ArrayList<>();
        boolean contentReady = validateContent(movie, blockers);
        boolean publishable = contentReady && validateApproval(movie, blockers);

        List<MovieLaunchReadinessResponse.ShowtimeReadiness> items = new ArrayList<>();
        long openableDrafts = 0;
        long draftCount = 0;
        long openCount = 0;
        for (Showtime showtime : showtimes) {
            if (showtime.getStatus() == ShowtimeStatus.OPEN_FOR_BOOKING) {
                openCount++;
                items.add(new MovieLaunchReadinessResponse.ShowtimeReadiness(
                        showtime.getPublicId(), showtime.getStatus().name(), showtime.getStartTime(),
                        true, List.of()));
                continue;
            }

            draftCount++;
            ShowtimeOpeningPolicy.Evaluation evaluation = openingPolicy.evaluate(showtime, now);
            List<MovieLaunchReadinessResponse.LaunchIssue> itemBlockers = evaluation.blockers().stream()
                    .map(issue -> new MovieLaunchReadinessResponse.LaunchIssue(
                            issue.code(), issue.message(), actionFor(issue.code()), showtime.getPublicId()))
                    .toList();
            if (evaluation.openable()) {
                openableDrafts++;
            }
            items.add(new MovieLaunchReadinessResponse.ShowtimeReadiness(
                    showtime.getPublicId(), showtime.getStatus().name(), showtime.getStartTime(),
                    evaluation.openable(), itemBlockers));
        }

        if (movie.getStatus() == MovieStatus.UPCOMING
                && movie.getReleaseDate() != null
                && !movie.getReleaseDate().isAfter(LocalDate.now(clock))
                && !approvalPolicy.hasOperationalShowtime(movie.getId())) {
            warnings.add(new MovieLaunchReadinessResponse.LaunchIssue(
                    "RELEASE_DATE_REACHED_WITHOUT_SHOWTIME",
                    "Release date has arrived, but no operational showtime is available to start the movie.",
                    "CREATE_SHOWTIME", null));
        }

        if ((movie.getStatus() == MovieStatus.UPCOMING || movie.getStatus() == MovieStatus.NOW_SHOWING)
                && showtimes.isEmpty()) {
            warnings.add(new MovieLaunchReadinessResponse.LaunchIssue(
                    "NO_FUTURE_SHOWTIME", "No future draft or open showtime is available.",
                    "CREATE_SHOWTIME", null));
        }

        if (movie.getStatus() == MovieStatus.NOW_SHOWING && openCount == 0) {
            blockers.add(new MovieLaunchReadinessResponse.LaunchIssue(
                    "NO_OPEN_SHOWTIME", "Movie is now showing but has no showtime open for booking.",
                    "OPEN_SHOWTIME", null));
        }

        return new MovieLaunchReadinessResponse(
                movie.getPublicId(), movie.getStatus(), contentReady, publishable, openCount > 0,
                showtimes.size(), draftCount, openCount, openableDrafts,
                draftCount - openableDrafts, List.copyOf(blockers), List.copyOf(warnings),
                List.copyOf(items));
    }

    private boolean validateContent(
            Movie movie, List<MovieLaunchReadinessResponse.LaunchIssue> blockers) {
        try {
            movieService.validatePublishConditions(movie.getId());
            return true;
        } catch (BusinessException exception) {
            blockers.add(issue(exception, "COMPLETE_MOVIE", null));
            return false;
        }
    }

    private boolean validateApproval(
            Movie movie, List<MovieLaunchReadinessResponse.LaunchIssue> blockers) {
        if (movie.getStatus() != MovieStatus.DRAFT) {
            return movie.getStatus() == MovieStatus.UPCOMING
                    || movie.getStatus() == MovieStatus.NOW_SHOWING;
        }
        MovieApprovalPolicy.ApprovalDecision decision = approvalPolicy.evaluate(movie);
        decision.blockers().forEach(message -> blockers.add(
                new MovieLaunchReadinessResponse.LaunchIssue(
                        "MOVIE_APPROVAL_BLOCKED", message, "REVIEW_MOVIE", null)));
        return decision.targetStatus() != null && decision.blockers().isEmpty();
    }

    private MovieLaunchReadinessResponse.LaunchIssue issue(
            BusinessException exception, String action, String showtimePublicId) {
        ErrorCode code = exception.getErrorCode();
        return new MovieLaunchReadinessResponse.LaunchIssue(
                code == null ? "VALIDATION_FAILED" : code.name(),
                exception.getMessage(), action, showtimePublicId);
    }

    private String actionFor(String code) {
        if (ErrorCode.PRICING_INCOMPLETE.name().equals(code)
                || ErrorCode.SHOWTIME_PRICE_MISSING.name().equals(code)) {
            return "FIX_PRICING";
        }
        if (ErrorCode.MOVIE_NOT_AVAILABLE_FOR_SCHEDULING.name().equals(code)) {
            return "PUBLISH_MOVIE";
        }
        return "REVIEW_SHOWTIME";
    }
}
