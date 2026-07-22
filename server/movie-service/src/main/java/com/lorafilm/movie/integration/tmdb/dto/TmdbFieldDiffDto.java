package com.lorafilm.movie.integration.tmdb.dto;

public record TmdbFieldDiffDto(
        String field,
        String label,
        String currentValue,
        String providerValue,
        boolean changed) {
}
