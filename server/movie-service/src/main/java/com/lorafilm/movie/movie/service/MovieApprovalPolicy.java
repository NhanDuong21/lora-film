package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MovieApprovalPolicy {

    public static final List<ShowtimeStatus> PUBLISHED_SHOWTIME_STATUSES = List.of(
            ShowtimeStatus.OPEN_FOR_BOOKING,
            ShowtimeStatus.CLOSED);

    private final MovieLifecyclePolicy lifecyclePolicy;
    private final ShowtimeRepository showtimeRepository;

    public MovieApprovalPolicy(
            MovieLifecyclePolicy lifecyclePolicy,
            ShowtimeRepository showtimeRepository) {
        this.lifecyclePolicy = lifecyclePolicy;
        this.showtimeRepository = showtimeRepository;
    }

    public ApprovalDecision evaluate(Movie movie) {
        List<String> blockers = new ArrayList<>();
        if (movie == null || movie.getReleaseDate() == null) {
            blockers.add("Vui lòng chọn ngày bắt đầu khai thác tại rạp trước khi duyệt phim.");
            return new ApprovalDecision(null, List.copyOf(blockers));
        }

        MovieStatus targetStatus = MovieStatus.UPCOMING;
        blockers.addAll(lifecyclePolicy.getTransitionViolations(movie, targetStatus));
        return new ApprovalDecision(targetStatus, List.copyOf(blockers));
    }

    public void validateApprovalTarget(Movie movie, MovieStatus requestedTarget) {
        ApprovalDecision decision = evaluate(movie);
        List<String> violations = new ArrayList<>(decision.blockers());
        if (decision.targetStatus() != null && decision.targetStatus() != requestedTarget) {
            violations.add("Trạng thái cần duyệt không phù hợp với ngày bắt đầu khai thác tại rạp.");
        }
        if (!violations.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_MOVIE_STATUS_TRANSITION,
                    String.join(" ", violations));
        }
    }

    public void validateNowShowingSchedule(Movie movie) {
        if (!hasPublishedShowtime(movie == null ? null : movie.getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_MOVIE_STATUS_TRANSITION,
                    "Muốn chuyển sang Đang chiếu, phim phải có ít nhất một suất chiếu hiện tại hoặc tương lai đã được công bố.");
        }
    }

    public boolean hasPublishedShowtime(Long movieId) {
        return movieId != null && showtimeRepository
                .existsByMovieIdAndStatusInAndEndTimeAfterAndDeletedAtIsNull(
                        movieId,
                        PUBLISHED_SHOWTIME_STATUSES,
                        lifecyclePolicy.currentInstant());
    }

    public record ApprovalDecision(MovieStatus targetStatus, List<String> blockers) {
    }
}
