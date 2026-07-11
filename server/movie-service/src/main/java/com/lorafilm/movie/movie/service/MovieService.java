package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;

public interface MovieService {
    PageResponse<MovieDto> getMovies(String status, Long genreId, String keyword, String city, Long cinemaId, java.time.LocalDate date, int page, int size, String sort);
    MovieDto getMovieBySlug(String slug);
}
