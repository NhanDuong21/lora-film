package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MovieLifecyclePolicy {

    private static final Map<MovieStatus, Set<MovieStatus>> ALLOWED_TRANSITIONS = buildTransitions();

    private final Clock clock;

    public MovieLifecyclePolicy() {
        this(Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Autowired
    public MovieLifecyclePolicy(Clock clock) {
        this.clock = clock;
    }

    public void validateTransition(Movie movie, MovieStatus targetStatus) {
        List<String> violations = getTransitionViolations(movie, targetStatus);
        if (!violations.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_MOVIE_STATUS_TRANSITION, String.join(" ", violations));
        }
    }

    public List<String> getTransitionViolations(Movie movie, MovieStatus targetStatus) {
        List<String> violations = new ArrayList<>();
        if (movie == null || movie.getStatus() == null || targetStatus == null) {
            violations.add("Hệ thống chưa xác định được trạng thái hiện tại hoặc trạng thái cần chuyển đến.");
            return violations;
        }

        Set<MovieStatus> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(movie.getStatus(), Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            violations.add("Không thể thực hiện thay đổi trạng thái này trong vòng đời hiện tại của phim.");
            return violations;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate releaseDate = movie.getReleaseDate();
        LocalDate endDate = movie.getEndDate();

        boolean targetUsesReleaseWindow = targetStatus == MovieStatus.UPCOMING
                || targetStatus == MovieStatus.NOW_SHOWING
                || targetStatus == MovieStatus.ENDED;
        if (targetUsesReleaseWindow && releaseDate != null && endDate != null && endDate.isBefore(releaseDate)) {
            violations.add("Ngày kết thúc khai thác không được trước ngày bắt đầu khai thác.");
        }

        if (targetStatus == MovieStatus.UPCOMING) {
            if (releaseDate == null || !releaseDate.isAfter(today)) {
                violations.add("Muốn chuyển sang Sắp chiếu, ngày bắt đầu khai thác phải sau hôm nay.");
            }
        } else if (targetStatus == MovieStatus.NOW_SHOWING) {
            if (releaseDate == null || releaseDate.isAfter(today)) {
                violations.add("Muốn chuyển sang Đang chiếu, ngày bắt đầu khai thác phải là hôm nay hoặc trước đó.");
            }
            if (endDate != null && endDate.isBefore(today)) {
                violations.add("Không thể chuyển sang Đang chiếu khi đợt khai thác đã kết thúc.");
            }
        } else if (targetStatus == MovieStatus.ENDED) {
            if (endDate == null || endDate.isAfter(today)) {
                violations.add("Muốn kết thúc phim, ngày kết thúc khai thác phải là hôm nay hoặc trước đó.");
            }
        }

        return List.copyOf(violations);
    }

    public boolean isAllowed(MovieStatus currentStatus, MovieStatus targetStatus) {
        return currentStatus != null
                && targetStatus != null
                && ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus);
    }

    public LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    public Instant currentInstant() {
        return Instant.now(clock);
    }

    private static Map<MovieStatus, Set<MovieStatus>> buildTransitions() {
        Map<MovieStatus, Set<MovieStatus>> transitions = new EnumMap<>(MovieStatus.class);
        transitions.put(MovieStatus.DRAFT, Set.of(
                MovieStatus.UPCOMING,
                MovieStatus.NOW_SHOWING,
                MovieStatus.INACTIVE));
        transitions.put(MovieStatus.UPCOMING, Set.of(MovieStatus.NOW_SHOWING, MovieStatus.INACTIVE));
        transitions.put(MovieStatus.NOW_SHOWING, Set.of(MovieStatus.ENDED, MovieStatus.INACTIVE));
        transitions.put(MovieStatus.ENDED, Set.of(MovieStatus.INACTIVE, MovieStatus.UPCOMING));
        transitions.put(MovieStatus.INACTIVE, Set.of(MovieStatus.DRAFT));
        return Map.copyOf(transitions);
    }
}
