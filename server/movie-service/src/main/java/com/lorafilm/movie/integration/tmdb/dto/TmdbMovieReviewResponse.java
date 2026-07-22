package com.lorafilm.movie.integration.tmdb.dto;

import com.lorafilm.movie.movie.dto.MovieReadinessDto;

import java.time.LocalDateTime;
import java.util.List;

public record TmdbMovieReviewResponse(
        String source,
        Long tmdbId,
        String reviewStatus,
        boolean canApprove,
        List<String> approvalBlockers,
        MovieReadinessDto readiness,
        LocalDateTime appliedTmdbLastUpdated,
        LocalDateTime providerLastUpdated,
        boolean hasProviderChanges,
        List<TmdbFieldDiffDto> scalarDiffs,
        List<TmdbCollectionDiffDto> collectionDiffs) {
}
