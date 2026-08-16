package com.lorafilm.movie.movie.dto.people;

import java.time.LocalDate;
import java.util.List;

public record PublicPersonDetailResponse(
        String id,
        String slug,
        String name,
        String originalName,
        String profileImageUrl,
        String biography,
        LocalDate birthDate,
        String placeOfBirth,
        List<String> roles,
        List<PublicPersonMovieResponse> availableMovies,
        List<PublicPersonMovieResponse> upcomingMovies,
        List<PublicPersonMovieResponse> otherCredits) {
}
