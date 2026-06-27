package com.project.movieservice.service;

import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.dto.AdminMovieListItemResponse;
import com.project.movieservice.dto.AdminMovieDetailResponse;

public interface MovieService {
    MoviePageResponse<MovieListItemResponse> getMovies(
            String pageStr, String sizeStr, String search, String status, String genreIdStr,
            String releaseFrom, String releaseTo, String sort);

    MovieDetailResponse getMovieDetail(String movieIdStr);

    MoviePageResponse<AdminMovieListItemResponse> getAdminMovies(
            String pageStr, String sizeStr, String search, String status, String genreIdStr,
            String releaseFrom, String releaseTo, String sort);

    AdminMovieDetailResponse getAdminMovieDetail(String movieIdStr);

    com.project.movieservice.dto.MovieCreatedResponse createMovie(com.project.movieservice.dto.MovieCreateRequest request);

    com.project.movieservice.dto.MovieUpdatedResponse updateMovie(String movieIdStr, com.project.movieservice.dto.MovieUpdateRequest request);

    com.project.movieservice.dto.MovieStatusResponse updateMovieStatus(String movieIdStr, com.project.movieservice.dto.MovieStatusUpdateRequest request);
}
