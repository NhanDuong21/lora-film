package com.project.analyticsservice.service.impl;

import com.project.analyticsservice.dto.*;
import com.project.analyticsservice.entity.MovieRevenueStat;
import com.project.analyticsservice.entity.MovieDailyRevenueStat;
import com.project.analyticsservice.enumtype.AnalyticsQueryMode;
import com.project.analyticsservice.enumtype.TopMovieMetric;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.MovieDailyRevenueStatRepository;
import com.project.analyticsservice.repository.MovieRevenueStatRepository;
import com.project.analyticsservice.service.MovieAnalyticsService;
import com.project.analyticsservice.dto.MovieDateRangeAggregateProjection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieAnalyticsServiceImpl implements MovieAnalyticsService {

    private final MovieRevenueStatRepository movieRevenueStatRepository;
    private final MovieDailyRevenueStatRepository movieDailyRevenueStatRepository;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("totalRevenue", "totalTicketsSold", "movieTitle");
    private static final Set<String> ALLOWED_DIRECTIONS = Set.of("asc", "desc");
    private static final String DEFAULT_CURRENCY = "VND";

    public MovieAnalyticsServiceImpl(MovieRevenueStatRepository movieRevenueStatRepository,
                                     MovieDailyRevenueStatRepository movieDailyRevenueStatRepository) {
        this.movieRevenueStatRepository = movieRevenueStatRepository;
        this.movieDailyRevenueStatRepository = movieDailyRevenueStatRepository;
    }

    @Override
    public MovieRevenueListResponse getMovieRevenueList(
            Integer page,
            Integer size,
            Long movieId,
            String movieTitle,
            String startDateStr,
            String endDateStr,
            String sortBy,
            String direction) {

        // 1. Validation & Defaulting for pagination/sort
        int pageVal = (page != null) ? page : 0;
        int sizeVal = (size != null) ? size : 10;
        if (pageVal < 0) {
            throw new BusinessException("Page index must not be less than zero", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (sizeVal < 1 || sizeVal > 100) {
            throw new BusinessException("Page size must be between 1 and 100", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        String sortByVal = (StringUtils.hasText(sortBy)) ? sortBy : "totalRevenue";
        String dirVal = (StringUtils.hasText(direction)) ? direction.toLowerCase() : "desc";

        if (!ALLOWED_SORT_FIELDS.contains(sortByVal) || !ALLOWED_DIRECTIONS.contains(dirVal)) {
            throw new BusinessException("Invalid sort field or direction", "ANALYTICS_INVALID_SORT", HttpStatus.BAD_REQUEST);
        }

        Sort.Direction sortDirection = dirVal.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageVal, sizeVal, Sort.by(sortDirection, sortByVal));

        // 2. Resolve query mode & dates
        AnalyticsQueryMode mode = resolveQueryMode(startDateStr, endDateStr);
        String cleanTitle = (StringUtils.hasText(movieTitle)) ? movieTitle.trim() : null;

        if (mode == AnalyticsQueryMode.LIFETIME) {
            // Lifetime mode: query movie_revenue_stats
            Page<MovieRevenueStat> statsPage = movieRevenueStatRepository.searchLifetime(movieId, cleanTitle, pageable);
            
            List<MovieRevenueListItemResponse> content = statsPage.getContent().stream()
                    .map(item -> new MovieRevenueListItemResponse(
                            item.getMovieId(),
                            item.getMovieTitle(),
                            item.getTotalTicketsSold(),
                            item.getTotalRevenue(),
                            DEFAULT_CURRENCY,
                            item.getUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            return new MovieRevenueListResponse(
                    "LIFETIME",
                    null,
                    content,
                    statsPage.getNumber(),
                    statsPage.getSize(),
                    statsPage.getTotalElements(),
                    statsPage.getTotalPages(),
                    statsPage.isFirst(),
                    statsPage.isLast()
            );
        } else {
            // Date Range mode: aggregate from movie_daily_revenue_stats
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            validateDateRange(startDate, endDate);

            Page<MovieDateRangeAggregateProjection> aggregatePage = movieDailyRevenueStatRepository
                    .aggregateMovieRevenueForDateRangeWithFilters(startDate, endDate, movieId, cleanTitle, pageable);

            List<MovieRevenueListItemResponse> content = aggregatePage.getContent().stream()
                    .map(item -> new MovieRevenueListItemResponse(
                            item.getMovieId(),
                            item.getMovieTitle(),
                            item.getTotalTicketsSold() != null ? item.getTotalTicketsSold().intValue() : 0,
                            item.getTotalRevenue() != null ? item.getTotalRevenue() : BigDecimal.ZERO,
                            DEFAULT_CURRENCY,
                            item.getLastUpdatedAt()
                    ))
                    .collect(Collectors.toList());

            AnalyticsPeriodResponse period = new AnalyticsPeriodResponse(startDateStr, endDateStr);

            return new MovieRevenueListResponse(
                    "DATE_RANGE",
                    period,
                    content,
                    aggregatePage.getNumber(),
                    aggregatePage.getSize(),
                    aggregatePage.getTotalElements(),
                    aggregatePage.getTotalPages(),
                    aggregatePage.isFirst(),
                    aggregatePage.isLast()
            );
        }
    }

    @Override
    public MovieRevenueDetailResponse getMovieRevenueDetail(Long movieId, String startDateStr, String endDateStr) {
        if (movieId == null || movieId <= 0) {
            throw new BusinessException("Movie ID must be greater than 0", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }

        AnalyticsQueryMode mode = resolveQueryMode(startDateStr, endDateStr);

        if (mode == AnalyticsQueryMode.LIFETIME) {
            MovieRevenueStat stat = movieRevenueStatRepository.findByMovieId(movieId)
                    .orElseThrow(() -> new BusinessException("Movie revenue statistics not found",
                            "ANALYTICS_MOVIE_STATS_NOT_FOUND", HttpStatus.NOT_FOUND));

            BigDecimal avg = calculateAverageRevenuePerTicket(stat.getTotalRevenue(), stat.getTotalTicketsSold());

            return new MovieRevenueDetailResponse(
                    stat.getMovieId(),
                    stat.getMovieTitle(),
                    "LIFETIME",
                    null,
                    null,
                    stat.getTotalTicketsSold(),
                    stat.getTotalRevenue(),
                    avg,
                    DEFAULT_CURRENCY,
                    stat.getUpdatedAt()
            );
        } else {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            validateDateRange(startDate, endDate);

            MovieDateRangeAggregateProjection proj = movieDailyRevenueStatRepository
                    .aggregateMovieRevenueForDateRangeAndMovieId(movieId, startDate, endDate)
                    .orElseThrow(() -> new BusinessException("Movie revenue statistics not found",
                            "ANALYTICS_MOVIE_STATS_NOT_FOUND", HttpStatus.NOT_FOUND));

            int ticketsSold = proj.getTotalTicketsSold() != null ? proj.getTotalTicketsSold().intValue() : 0;
            BigDecimal revenue = proj.getTotalRevenue() != null ? proj.getTotalRevenue() : BigDecimal.ZERO;
            
            // Check if there are indeed records (if SUM of tickets and revenue is null and date range has no records, we throw not found)
            if (proj.getLastUpdatedAt() == null) {
                throw new BusinessException("Movie revenue statistics not found",
                        "ANALYTICS_MOVIE_STATS_NOT_FOUND", HttpStatus.NOT_FOUND);
            }

            BigDecimal avg = calculateAverageRevenuePerTicket(revenue, ticketsSold);

            return new MovieRevenueDetailResponse(
                    proj.getMovieId(),
                    proj.getMovieTitle(),
                    "DATE_RANGE",
                    startDateStr,
                    endDateStr,
                    ticketsSold,
                    revenue,
                    avg,
                    DEFAULT_CURRENCY,
                    proj.getLastUpdatedAt()
            );
        }
    }

    @Override
    public MovieRevenueTrendResponse getMovieRevenueTrend(Long movieId, String startDateStr, String endDateStr, Boolean includeEmptyDates) {
        if (movieId == null || movieId <= 0) {
            throw new BusinessException("Movie ID must be greater than 0", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (!StringUtils.hasText(startDateStr) || !StringUtils.hasText(endDateStr)) {
            throw new BusinessException("Both startDate and endDate are required for trend query",
                    "ANALYTICS_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
        }

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);
        validateDateRange(startDate, endDate);

        boolean includeEmpty = (includeEmptyDates != null) ? includeEmptyDates : true;

        List<MovieDailyRevenueStat> dailyStats = movieDailyRevenueStatRepository
                .findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(movieId, startDate, endDate);

        // Resolve movieTitle
        String movieTitle = null;
        if (!dailyStats.isEmpty()) {
            // Get from the latest record in range (sorted by statDate asc, so last record is the latest)
            movieTitle = dailyStats.get(dailyStats.size() - 1).getMovieTitle();
        } else {
            // Fallback to lifetime table
            MovieRevenueStat lifetimeStat = movieRevenueStatRepository.findByMovieId(movieId)
                    .orElseThrow(() -> new BusinessException("Movie revenue statistics not found",
                            "ANALYTICS_MOVIE_STATS_NOT_FOUND", HttpStatus.NOT_FOUND));
            movieTitle = lifetimeStat.getMovieTitle();
        }

        List<MovieRevenueTrendItemResponse> statistics = new ArrayList<>();

        if (includeEmpty) {
            Map<LocalDate, MovieDailyRevenueStat> statMap = dailyStats.stream()
                    .collect(Collectors.toMap(MovieDailyRevenueStat::getStatDate, s -> s));

            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (statMap.containsKey(current)) {
                    MovieDailyRevenueStat s = statMap.get(current);
                    statistics.add(new MovieRevenueTrendItemResponse(
                            s.getStatDate().toString(),
                            s.getTicketsSold(),
                            s.getRevenue()
                    ));
                } else {
                    statistics.add(new MovieRevenueTrendItemResponse(
                            current.toString(),
                            0,
                            BigDecimal.ZERO
                    ));
                }
                current = current.plusDays(1);
            }
        } else {
            for (MovieDailyRevenueStat s : dailyStats) {
                statistics.add(new MovieRevenueTrendItemResponse(
                        s.getStatDate().toString(),
                        s.getTicketsSold(),
                        s.getRevenue()
                ));
            }
        }

        return new MovieRevenueTrendResponse(
                movieId,
                movieTitle,
                startDateStr,
                endDateStr,
                DEFAULT_CURRENCY,
                statistics
        );
    }

    @Override
    public TopMoviesResponse getTopMovies(
            String metricStr,
            Integer limit,
            String direction,
            String startDateStr,
            String endDateStr) {

        // Validate Metric
        TopMovieMetric metric = TopMovieMetric.REVENUE;
        if (StringUtils.hasText(metricStr)) {
            try {
                metric = TopMovieMetric.valueOf(metricStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BusinessException("Invalid metric", "ANALYTICS_INVALID_METRIC", HttpStatus.BAD_REQUEST);
            }
        }

        // Validate Limit
        int limitVal = (limit != null) ? limit : 10;
        if (limitVal < 1 || limitVal > 50) {
            throw new BusinessException("Limit must be between 1 and 50", "ANALYTICS_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        // Validate Direction
        String dirVal = (StringUtils.hasText(direction)) ? direction.toLowerCase() : "desc";
        if (!ALLOWED_DIRECTIONS.contains(dirVal)) {
            throw new BusinessException("Invalid sort direction", "ANALYTICS_INVALID_SORT", HttpStatus.BAD_REQUEST);
        }

        Sort.Direction sortDirection = dirVal.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        // Map metric to sort field
        String sortField = (metric == TopMovieMetric.REVENUE) ? "totalRevenue" : "totalTicketsSold";
        Pageable pageable = PageRequest.of(0, limitVal, Sort.by(sortDirection, sortField));

        AnalyticsQueryMode mode = resolveQueryMode(startDateStr, endDateStr);
        List<TopMovieItemResponse> movies = new ArrayList<>();
        LocalDateTime lastUpdated = null;
        AnalyticsPeriodResponse period = null;

        if (mode == AnalyticsQueryMode.LIFETIME) {
            Page<MovieRevenueStat> pageResult = movieRevenueStatRepository.findAll(pageable);
            int rank = 1;
            for (MovieRevenueStat item : pageResult.getContent()) {
                movies.add(new TopMovieItemResponse(
                        rank++,
                        item.getMovieId(),
                        item.getMovieTitle(),
                        item.getTotalTicketsSold(),
                        item.getTotalRevenue()
                ));
                if (lastUpdated == null || item.getUpdatedAt().isAfter(lastUpdated)) {
                    lastUpdated = item.getUpdatedAt();
                }
            }
        } else {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            validateDateRange(startDate, endDate);

            Page<MovieDateRangeAggregateProjection> pageResult = movieDailyRevenueStatRepository
                    .aggregateMovieRevenueForDateRangeWithFilters(startDate, endDate, null, null, pageable);

            int rank = 1;
            for (MovieDateRangeAggregateProjection item : pageResult.getContent()) {
                movies.add(new TopMovieItemResponse(
                        rank++,
                        item.getMovieId(),
                        item.getMovieTitle(),
                        item.getTotalTicketsSold() != null ? item.getTotalTicketsSold().intValue() : 0,
                        item.getTotalRevenue() != null ? item.getTotalRevenue() : BigDecimal.ZERO
                ));
                if (item.getLastUpdatedAt() != null && (lastUpdated == null || item.getLastUpdatedAt().isAfter(lastUpdated))) {
                    lastUpdated = item.getLastUpdatedAt();
                }
            }
            period = new AnalyticsPeriodResponse(startDateStr, endDateStr);
        }

        return new TopMoviesResponse(
                metric.name(),
                mode.name(),
                period,
                DEFAULT_CURRENCY,
                movies,
                lastUpdated
        );
    }

    private AnalyticsQueryMode resolveQueryMode(String startDateStr, String endDateStr) {
        boolean hasStart = StringUtils.hasText(startDateStr);
        boolean hasEnd = StringUtils.hasText(endDateStr);

        if (!hasStart && !hasEnd) {
            return AnalyticsQueryMode.LIFETIME;
        }

        if (hasStart && hasEnd) {
            // Validate formats
            try {
                LocalDate.parse(startDateStr);
                LocalDate.parse(endDateStr);
            } catch (DateTimeParseException ex) {
                throw new BusinessException("Invalid date format", "ANALYTICS_INVALID_DATE_FORMAT", HttpStatus.BAD_REQUEST);
            }
            return AnalyticsQueryMode.DATE_RANGE;
        }

        throw new BusinessException("Both startDate and endDate must be provided together or completely omitted",
                "ANALYTICS_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Start date cannot be after end date", "ANALYTICS_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
        }

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysBetween >= 92) { // range exceeds 92 days (since inclusive date, it means daysBetween is 92 or more)
            throw new BusinessException("Analytics date range must not exceed 92 days",
                    "ANALYTICS_DATE_RANGE_TOO_LARGE", HttpStatus.BAD_REQUEST);
        }
    }

    private BigDecimal calculateAverageRevenuePerTicket(BigDecimal totalRevenue, int totalTicketsSold) {
        if (totalTicketsSold == 0) {
            return new BigDecimal("0.00");
        }
        return totalRevenue.divide(BigDecimal.valueOf(totalTicketsSold), 2, RoundingMode.HALF_UP);
    }
}
