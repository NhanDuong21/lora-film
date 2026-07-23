package com.lorafilm.movie.integration.tmdb.dto;

public enum TmdbImportOutcome {
    CREATED,
    ALREADY_IMPORTED,
    DELETED_TOMBSTONE,
    REJECTED_BY_PROVIDER
}
