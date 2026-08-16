package com.lorafilm.movie.movie.dto.people;

import java.time.LocalDate;

public record PublicPersonMovieResponse(
        String id,
        String slug,
        String title,
        String posterUrl,
        LocalDate releaseDate,
        String availability,
        String role,
        String characterName) {
}
