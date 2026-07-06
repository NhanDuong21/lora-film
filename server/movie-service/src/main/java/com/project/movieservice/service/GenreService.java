package com.project.movieservice.service;

import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;

import java.util.List;

public interface GenreService {
    
    List<GenreResponse> getGenres(boolean isAdmin);
    
    GenreResponse getGenreById(Integer genreId, boolean isAdmin);
    
    GenreResponse createGenre(GenreCreateRequest request);
    
    GenreResponse updateGenre(Integer genreId, GenreUpdateRequest request);

    void softDeleteGenre(Integer genreId);
}
