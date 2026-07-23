package com.lorafilm.movie.integration.tmdb.dto;

public record TmdbImportResult(
        Long tmdbId,
        TmdbImportOutcome outcome,
        String moviePublicId,
        String message) {
}
