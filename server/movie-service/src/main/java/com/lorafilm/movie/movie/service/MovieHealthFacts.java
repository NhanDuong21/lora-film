package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.dto.MovieDto;

import java.time.LocalDate;

/**
 * Query-friendly, non-persistent facts used by the canonical movie health policy.
 */
public record MovieHealthFacts(
        boolean hasGenres,
        boolean hasActiveVersion,
        boolean hasPrimaryPoster,
        String title,
        LocalDate releaseDate,
        AgeRating ageRating,
        Integer durationMinutes) {

    public static MovieHealthFacts from(
            MovieDto movie,
            boolean hasActiveVersion,
            boolean hasPrimaryPoster) {
        if (movie == null) {
            return new MovieHealthFacts(
                    false,
                    hasActiveVersion,
                    hasPrimaryPoster,
                    null,
                    null,
                    null,
                    null);
        }

        return new MovieHealthFacts(
                movie.getGenres() != null && !movie.getGenres().isEmpty(),
                hasActiveVersion,
                hasPrimaryPoster,
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getAgeRating(),
                movie.getDurationMinutes());
    }

    public static MovieHealthFacts from(
            Movie movie,
            boolean hasGenres,
            boolean hasActiveVersion,
            boolean hasPrimaryPoster) {
        if (movie == null) {
            return new MovieHealthFacts(
                    hasGenres,
                    hasActiveVersion,
                    hasPrimaryPoster,
                    null,
                    null,
                    null,
                    null);
        }

        return new MovieHealthFacts(
                hasGenres,
                hasActiveVersion,
                hasPrimaryPoster,
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getAgeRating(),
                movie.getDurationMinutes());
    }
}
