package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class MovieApprovalPolicy {

    public static final List<ShowtimeStatus> OPERATIONAL_SHOWTIME_STATUSES = List.of(
            ShowtimeStatus.DRAFT,
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

        LocalDate today = lifecyclePolicy.currentDate();
        MovieStatus targetStatus = movie.getReleaseDate().isAfter(today)
                ? MovieStatus.UPCOMING
                : MovieStatus.NOW_SHOWING;

        blockers.addAll(lifecyclePolicy.getTransitionViolations(movie, targetStatus));
        if (targetStatus == MovieStatus.NOW_SHOWING && !hasOperationalShowtime(movie.getId())) {
            blockers.add("Muốn chuyển sang Đang chiếu, phim phải có ít nhất một suất chiếu hiện tại hoặc tương lai chưa bị hủy.");
        }
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
        if (!hasOperationalShowtime(movie == null ? null : movie.getId())) {
            throw new BusinessException(
                    ErrorCode.INVALID_MOVIE_STATUS_TRANSITION,
                    "Muốn chuyển sang Đang chiếu, phim phải có ít nhất một suất chiếu hiện tại hoặc tương lai chưa bị hủy.");
        }
    }

    public boolean hasOperationalShowtime(Long movieId) {
        return movieId != null && showtimeRepository
                .existsByMovieIdAndStatusInAndEndTimeAfterAndDeletedAtIsNull(
                        movieId,
                        OPERATIONAL_SHOWTIME_STATUSES,
                        lifecyclePolicy.currentInstant());
    }

    public record ApprovalDecision(MovieStatus targetStatus, List<String> blockers) {
    }
}
