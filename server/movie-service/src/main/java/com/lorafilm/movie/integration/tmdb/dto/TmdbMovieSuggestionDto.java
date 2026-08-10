package com.lorafilm.movie.integration.tmdb.dto;

import java.time.LocalDate;

public record TmdbMovieSuggestionDto(
        Long tmdbId,
        String title,
        String originalTitle,
        LocalDate originalReleaseDate,
        String posterUrl,
        String overview,
        boolean alreadyImported,
        String localMoviePublicId,
        String localMovieStatus) {
}
