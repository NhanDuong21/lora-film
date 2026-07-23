package com.lorafilm.movie.integration.tmdb.dto;

import java.util.List;

public record TmdbCollectionDiffDto(
        String field,
        String label,
        List<String> currentValues,
        List<String> providerValues,
        List<String> added,
        List<String> removed,
        boolean changed) {
}
