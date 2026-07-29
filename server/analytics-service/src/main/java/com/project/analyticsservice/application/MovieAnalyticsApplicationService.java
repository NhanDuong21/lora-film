package com.project.analyticsservice.application;

import com.project.analyticsservice.dto.*;

public interface MovieAnalyticsApplicationService {

    MovieRevenueListResponse getMovieRevenueList(
            Integer page,
            Integer size,
            Long movieId,
            String movieTitle,
            String startDateStr,
            String endDateStr,
            String sortBy,
            String direction);

    MovieRevenueDetailResponse getMovieRevenueDetail(
            Long movieId,
            String startDateStr,
            String endDateStr);

    MovieRevenueTrendResponse getMovieRevenueTrend(
            Long movieId,
            String startDateStr,
            String endDateStr,
            Boolean includeEmptyDates);

    TopMoviesResponse getTopMovies(
            String metricStr,
            Integer limit,
            String direction,
            String startDateStr,
            String endDateStr);
}
