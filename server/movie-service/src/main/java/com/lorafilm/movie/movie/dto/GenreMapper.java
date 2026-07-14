package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.entity.Genre;
import org.springframework.stereotype.Component;

@Component
public class GenreMapper {
    public GenreResponse toResponse(Genre genre) {
        if (genre == null) return null;
        GenreResponse response = new GenreResponse();
        response.setPublicId(genre.getPublicId());
        response.setName(genre.getName());
        response.setSlug(genre.getSlug());
        response.setStatus(genre.getStatus());
        return response;
    }
}
