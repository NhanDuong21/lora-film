package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieDetailDto;
import com.lorafilm.movie.movie.dto.AdminMovieListQuery;
import com.lorafilm.movie.movie.dto.MovieBulkApprovalResponse;
import com.lorafilm.movie.movie.dto.TmdbQueueBreakdownResponse;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public interface MovieService {
    PageResponse<MovieDto> getMovies(AdminMovieListQuery query);
    MovieDetailDto getMovieByIdentifier(String identifier);
    MovieDto updateMovieStatus(String moviePublicId, MovieStatus targetStatus);
    MovieDto updateMovieStatus(String moviePublicId, MovieStatus targetStatus, String reason);
    MovieBulkApprovalResponse bulkApproveTmdbMovies(AdminMovieListQuery filter, int limit);
    TmdbQueueBreakdownResponse getTmdbQueueBreakdown(AdminMovieListQuery filter);
    void validatePublishConditions(Long movieId);
}
