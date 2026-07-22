package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.enums.MovieHealthStatus;
import com.lorafilm.movie.movie.dto.MovieReadinessDto;
import com.lorafilm.movie.movie.dto.ReadinessIssueDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MovieReadinessEvaluator {

    // Blockers
    public static final String NO_GENRE = "NO_GENRE";
    public static final String NO_ACTIVE_VERSION = "NO_ACTIVE_VERSION";
    public static final String NO_ACTIVE_PRIMARY_POSTER = "NO_ACTIVE_PRIMARY_POSTER";

    // Warnings
    public static final String INVALID_DURATION = "INVALID_DURATION";
    public static final String SUSPICIOUS_DURATION = "SUSPICIOUS_DURATION";
    public static final String MISSING_AGE_RATING = "MISSING_AGE_RATING";
    public static final String MISSING_RELEASE_DATE = "MISSING_RELEASE_DATE";
    public static final String MISSING_TITLE = "MISSING_TITLE";

    public MovieReadinessDto evaluate(MovieHealthFacts facts) {
        List<ReadinessIssueDto> blockers = new ArrayList<>();
        List<ReadinessIssueDto> warnings = new ArrayList<>();

        if (!facts.hasGenres()) {
            addIssue(blockers, warnings, IssueCatalog.NO_GENRE);
        }
        if (!facts.hasActiveVersion()) {
            addIssue(blockers, warnings, IssueCatalog.NO_ACTIVE_VERSION);
        }
        if (!facts.hasPrimaryPoster()) {
            addIssue(blockers, warnings, IssueCatalog.NO_ACTIVE_PRIMARY_POSTER);
        }

        if (facts.title() == null || facts.title().trim().isEmpty()) {
            addIssue(blockers, warnings, IssueCatalog.MISSING_TITLE);
        }
        if (facts.releaseDate() == null) {
            addIssue(blockers, warnings, IssueCatalog.MISSING_RELEASE_DATE);
        }
        if (facts.ageRating() == null) {
            addIssue(blockers, warnings, IssueCatalog.MISSING_AGE_RATING);
        }

        if (facts.durationMinutes() == null || facts.durationMinutes() <= 0) {
            addIssue(blockers, warnings, IssueCatalog.INVALID_DURATION);
        } else if (facts.durationMinutes() < 30) {
            addIssue(
                    blockers,
                    warnings,
                    IssueCatalog.SUSPICIOUS_DURATION,
                    "Cần kiểm tra thời lượng: " + facts.durationMinutes() + " phút.");
        }

        MovieHealthStatus healthStatus = !blockers.isEmpty()
                ? MovieHealthStatus.BLOCKED
                : !warnings.isEmpty() ? MovieHealthStatus.WARNING : MovieHealthStatus.READY;
        String classification = healthStatus == MovieHealthStatus.BLOCKED ? "INCOMPLETE" : "READY";

        return new MovieReadinessDto(healthStatus, classification, blockers, warnings);
    }

    public void validatePublishConditions(MovieHealthFacts facts) {
        MovieReadinessDto readiness = evaluate(facts);
        if (readiness.getHealthStatus() != MovieHealthStatus.BLOCKED) {
            return;
        }

        boolean missingGenre = hasBlocker(readiness, NO_GENRE);
        boolean missingActiveVersion = hasBlocker(readiness, NO_ACTIVE_VERSION);
        boolean missingPrimaryPoster = hasBlocker(readiness, NO_ACTIVE_PRIMARY_POSTER);

        if (missingGenre) {
            throw new BusinessException(
                    ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED,
                    "Movie must have at least 1 genre to be published");
        }
        if (missingActiveVersion && missingPrimaryPoster) {
            throw new BusinessException(
                    ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED,
                    "Movie must have at least one active version and one primary poster to publish");
        }
        if (missingActiveVersion) {
            throw new BusinessException(
                    ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED,
                    "Movie must have at least one active version to publish");
        }
        if (missingPrimaryPoster) {
            throw new BusinessException(
                    ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED,
                    "Movie must have at least one active primary poster to publish");
        }

        throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie is incomplete");
    }

    private boolean hasBlocker(MovieReadinessDto readiness, String code) {
        return readiness.getBlockers().stream().anyMatch(issue -> code.equals(issue.getCode()));
    }

    private void addIssue(
            List<ReadinessIssueDto> blockers,
            List<ReadinessIssueDto> warnings,
            IssueCatalog issue) {
        addIssue(blockers, warnings, issue, issue.message);
    }

    private void addIssue(
            List<ReadinessIssueDto> blockers,
            List<ReadinessIssueDto> warnings,
            IssueCatalog issue,
            String message) {
        ReadinessIssueDto issueDto = new ReadinessIssueDto(issue.code, message);
        if (issue.severity == Severity.BLOCKER) {
            blockers.add(issueDto);
        } else {
            warnings.add(issueDto);
        }
    }

    private enum Severity {
        BLOCKER,
        WARNING
    }

    private enum IssueCatalog {
        NO_GENRE(MovieReadinessEvaluator.NO_GENRE, Severity.BLOCKER, "Phim chưa có thể loại."),
        NO_ACTIVE_VERSION(MovieReadinessEvaluator.NO_ACTIVE_VERSION, Severity.BLOCKER, "Phim chưa có phiên bản đang hoạt động."),
        NO_ACTIVE_PRIMARY_POSTER(MovieReadinessEvaluator.NO_ACTIVE_PRIMARY_POSTER, Severity.BLOCKER, "Phim chưa có poster chính đang hoạt động."),
        MISSING_TITLE(MovieReadinessEvaluator.MISSING_TITLE, Severity.WARNING, "Cần kiểm tra lại: Thiếu tên phim."),
        MISSING_RELEASE_DATE(MovieReadinessEvaluator.MISSING_RELEASE_DATE, Severity.WARNING, "Cần kiểm tra lại: Thiếu ngày khởi chiếu."),
        MISSING_AGE_RATING(MovieReadinessEvaluator.MISSING_AGE_RATING, Severity.WARNING, "Cần kiểm tra lại: Thiếu phân loại độ tuổi."),
        INVALID_DURATION(MovieReadinessEvaluator.INVALID_DURATION, Severity.WARNING, "Cần kiểm tra lại: Thời lượng không hợp lệ."),
        SUSPICIOUS_DURATION(MovieReadinessEvaluator.SUSPICIOUS_DURATION, Severity.WARNING, "Cần kiểm tra thời lượng.");

        private final String code;
        private final Severity severity;
        private final String message;

        IssueCatalog(String code, Severity severity, String message) {
            this.code = code;
            this.severity = severity;
            this.message = message;
        }
    }
}
