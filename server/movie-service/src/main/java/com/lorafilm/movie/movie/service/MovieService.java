package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;

public interface MovieService {
    PageResponse<MovieDto> getMovies(String status, String keyword, int page, int size, String sort);
    MovieDto getMovieBySlug(String slug);
}
