package com.project.analyticsservice.application;

import com.project.analyticsservice.domain.service.MovieAnalyticsDomainService;
import com.project.analyticsservice.dto.MovieRevenueDetailResponse;
import com.project.analyticsservice.dto.MovieRevenueListResponse;
import com.project.analyticsservice.dto.MovieRevenueTrendResponse;
import com.project.analyticsservice.dto.TopMoviesResponse;
import org.springframework.stereotype.Service;

@Service
public class MovieAnalyticsApplicationServiceImpl implements MovieAnalyticsApplicationService {
    private final MovieAnalyticsDomainService domainService;

    public MovieAnalyticsApplicationServiceImpl(MovieAnalyticsDomainService domainService) {
        this.domainService = domainService;
    }

    @Override
    public MovieRevenueListResponse getMovieRevenueList(
            Integer page, Integer size, Long movieId, String movieTitle,
            String startDate, String endDate, String sortBy, String direction) {
        return domainService.getMovieRevenueList(
                page, size, movieId, movieTitle, startDate, endDate, sortBy, direction);
    }

    @Override
    public MovieRevenueDetailResponse getMovieRevenueDetail(
            Long movieId, String startDate, String endDate) {
        return domainService.getMovieRevenueDetail(movieId, startDate, endDate);
    }

    @Override
    public MovieRevenueTrendResponse getMovieRevenueTrend(
            Long movieId, String startDate, String endDate, Boolean includeEmptyDates) {
        return domainService.getMovieRevenueTrend(movieId, startDate, endDate, includeEmptyDates);
    }

    @Override
    public TopMoviesResponse getTopMovies(
            String metric, Integer limit, String direction, String startDate, String endDate) {
        return domainService.getTopMovies(metric, limit, direction, startDate, endDate);
    }
}
