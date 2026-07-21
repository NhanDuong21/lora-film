package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.dto.MovieDto;
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

    public MovieReadinessDto evaluate(MovieDto movie, boolean hasActiveVersion, boolean hasPrimaryPoster) {
        List<ReadinessIssueDto> blockers = new ArrayList<>();
        List<ReadinessIssueDto> warnings = new ArrayList<>();

        // 1. Evaluate Blockers
        if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
            blockers.add(new ReadinessIssueDto(NO_GENRE, "Phim chưa có thể loại."));
        }
        if (!hasActiveVersion) {
            blockers.add(new ReadinessIssueDto(NO_ACTIVE_VERSION, "Phim chưa có phiên bản đang hoạt động."));
        }
        if (!hasPrimaryPoster) {
            blockers.add(new ReadinessIssueDto(NO_ACTIVE_PRIMARY_POSTER, "Phim chưa có poster chính đang hoạt động."));
        }

        // 2. Evaluate Warnings
        if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
            warnings.add(new ReadinessIssueDto(MISSING_TITLE, "Cần kiểm tra lại: Thiếu tên phim."));
        }
        if (movie.getReleaseDate() == null) {
            warnings.add(new ReadinessIssueDto(MISSING_RELEASE_DATE, "Cần kiểm tra lại: Thiếu ngày khởi chiếu."));
        }
        if (movie.getAgeRating() == null) {
            warnings.add(new ReadinessIssueDto(MISSING_AGE_RATING, "Cần kiểm tra lại: Thiếu phân loại độ tuổi."));
        }

        if (movie.getDurationMinutes() == null || movie.getDurationMinutes() <= 0) {
            warnings.add(new ReadinessIssueDto(INVALID_DURATION, "Cần kiểm tra lại: Thời lượng không hợp lệ."));
        } else if (movie.getDurationMinutes() < 30) {
            warnings.add(new ReadinessIssueDto(SUSPICIOUS_DURATION, "Cần kiểm tra thời lượng: " + movie.getDurationMinutes() + " phút."));
        }

        String classification = blockers.isEmpty() ? "READY" : "INCOMPLETE";

        return new MovieReadinessDto(classification, blockers, warnings);
    }

    public void validatePublishConditions(MovieDto movie, boolean hasActiveVersion, boolean hasPrimaryPoster) {
        MovieReadinessDto readiness = evaluate(movie, hasActiveVersion, hasPrimaryPoster);
        if ("INCOMPLETE".equals(readiness.getClassification())) {
            // Find the specific error for backward compatibility with exceptions
            boolean hasGenre = readiness.getBlockers().stream().noneMatch(b -> b.getCode().equals(NO_GENRE));
            boolean activeVersion = readiness.getBlockers().stream().noneMatch(b -> b.getCode().equals(NO_ACTIVE_VERSION));
            boolean primaryPoster = readiness.getBlockers().stream().noneMatch(b -> b.getCode().equals(NO_ACTIVE_PRIMARY_POSTER));

            if (!hasGenre) {
                throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least 1 genre to be published");
            }
            if (!activeVersion && !primaryPoster) {
                throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least one active version and one primary poster to publish");
            }
            if (!activeVersion) {
                throw new BusinessException(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, "Movie must have at least one active version to publish");
            }
            if (!primaryPoster) {
                throw new BusinessException(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, "Movie must have at least one active primary poster to publish");
            }
            
            // Fallback
            throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie is incomplete");
        }
    }
}
