package com.project.movieservice.service;

import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.dto.MoviePageResponse;

public interface MovieService {
    MoviePageResponse<MovieListItemResponse> getMovies(
            String pageStr, String sizeStr, String search, String status, String genreIdStr,
            String releaseFrom, String releaseTo, String sort);

    MovieDetailResponse getMovieDetail(String movieIdStr);
}
