package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.entity.Movie;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MovieMapper {

    public MovieDto toDto(Movie movie, List<String> genres, String primaryPosterUrl) {
        if (movie == null) {
            return null;
        }
        MovieDto dto = new MovieDto();
        dto.setPublicId(movie.getPublicId());
        dto.setSlug(movie.getSlug());
        dto.setTitle(movie.getTitle());
        dto.setOriginalTitle(movie.getOriginalTitle());
        dto.setSynopsis(movie.getSynopsis());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setAgeRating(movie.getAgeRating());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setEndDate(movie.getEndDate());
        dto.setStatus(movie.getStatus());
        dto.setGenres(genres);
        dto.setPrimaryPoster(primaryPosterUrl);
        dto.setActiveSlug(movie.getActiveSlug());
        dto.setCountry(movie.getCountry());
        return dto;
    }
}
