package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.*;
import com.project.analyticsservice.entity.MoviePerformanceDaily;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.MoviePerformanceDailyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieAnalyticsDomainService {
    private static final Set<String> SORT_FIELDS =
            Set.of("totalRevenue", "totalTicketsSold", "movieTitle");
    private static final Set<String> DIRECTIONS = Set.of("asc", "desc");
    private static final String CURRENCY = "VND";

    private final MoviePerformanceDailyRepository repository;
    private final MetricMathService math;

    public MovieAnalyticsDomainService(
            MoviePerformanceDailyRepository repository,
            MetricMathService math) {
        this.repository = repository;
        this.math = math;
    }

    public MovieRevenueListResponse getMovieRevenueList(
            Integer page,
            Integer size,
            Long movieId,
            String movieTitle,
            String startDate,
            String endDate,
            String sortBy,
            String direction) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 10 : size;
        if (resolvedPage < 0 || resolvedSize < 1 || resolvedSize > 100) {
            throw invalid("Invalid pagination", "VALIDATION_ERROR");
        }
        String field = StringUtils.hasText(sortBy) ? sortBy : "totalRevenue";
        String resolvedDirection = StringUtils.hasText(direction) ? direction.toLowerCase() : "desc";
        if (!SORT_FIELDS.contains(field) || !DIRECTIONS.contains(resolvedDirection)) {
            throw invalid("Invalid sort field or direction", "ANALYTICS_INVALID_SORT");
        }

        DateSelection dates = dates(startDate, endDate);
        List<MovieAggregate> values = aggregate(load(dates)).stream()
                .filter(value -> movieId == null || Objects.equals(movieId, value.movieId))
                .filter(value -> !StringUtils.hasText(movieTitle)
                        || value.movieTitle.toLowerCase().contains(movieTitle.trim().toLowerCase()))
                .sorted(comparator(field, resolvedDirection))
                .toList();
        int from = Math.min(resolvedPage * resolvedSize, values.size());
        int to = Math.min(from + resolvedSize, values.size());
        List<MovieRevenueListItemResponse> content = values.subList(from, to).stream()
                .map(value -> new MovieRevenueListItemResponse(
                        value.movieId, value.movieTitle, safeInt(value.ticketCount),
                        math.money(value.revenue), CURRENCY, utc(value.updatedAt)))
                .toList();
        int totalPages = values.isEmpty() ? 0 : (values.size() + resolvedSize - 1) / resolvedSize;
        return new MovieRevenueListResponse(
                dates.mode,
                dates.period(),
                content,
                resolvedPage,
                resolvedSize,
                (long) values.size(),
                totalPages,
                resolvedPage == 0,
                resolvedPage >= Math.max(0, totalPages - 1));
    }

    public MovieRevenueDetailResponse getMovieRevenueDetail(
            Long movieId, String startDate, String endDate) {
        requireMovieId(movieId);
        DateSelection dates = dates(startDate, endDate);
        MovieAggregate value = aggregate(load(dates)).stream()
                .filter(item -> Objects.equals(item.movieId, movieId))
                .findFirst()
                .orElseThrow(() -> notFound());
        return new MovieRevenueDetailResponse(
                movieId, value.movieTitle, dates.mode,
                dates.start == null ? null : dates.start.toString(),
                dates.end == null ? null : dates.end.toString(),
                safeInt(value.ticketCount), math.money(value.revenue),
                math.money(math.ratio(value.revenue, value.ticketCount)),
                CURRENCY, utc(value.updatedAt));
    }

    public MovieRevenueTrendResponse getMovieRevenueTrend(
            Long movieId,
            String startDate,
            String endDate,
            Boolean includeEmptyDates) {
        requireMovieId(movieId);
        DateSelection dates = dates(startDate, endDate);
        if (dates.start == null) {
            throw invalid("startDate and endDate are required for trend query",
                    "ANALYTICS_INVALID_DATE_RANGE");
        }
        List<MoviePerformanceDaily> values = repository
                .findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(
                        movieId, dates.start, dates.end);
        if (values.isEmpty()) {
            throw notFound();
        }
        Map<LocalDate, MoviePerformanceDaily> byDate = values.stream()
                .collect(Collectors.toMap(
                        MoviePerformanceDaily::getStatDate,
                        Function.identity(),
                        (first, second) -> second));
        List<MovieRevenueTrendItemResponse> trend = new ArrayList<>();
        if (Boolean.FALSE.equals(includeEmptyDates)) {
            values.forEach(value -> trend.add(new MovieRevenueTrendItemResponse(
                    value.getStatDate().toString(), safeInt(value.getTicketCount()),
                    value.getNetRevenue())));
        } else {
            dates.start.datesUntil(dates.end.plusDays(1)).forEach(date -> {
                MoviePerformanceDaily value = byDate.get(date);
                trend.add(new MovieRevenueTrendItemResponse(
                        date.toString(),
                        value == null ? 0 : safeInt(value.getTicketCount()),
                        value == null ? BigDecimal.ZERO : value.getNetRevenue()));
            });
        }
        return new MovieRevenueTrendResponse(
                movieId, values.getLast().getMovieTitle(),
                dates.start.toString(), dates.end.toString(), CURRENCY, trend);
    }

    public TopMoviesResponse getTopMovies(
            String metric,
            Integer limit,
            String direction,
            String startDate,
            String endDate) {
        String resolvedMetric = StringUtils.hasText(metric) ? metric.toUpperCase() : "REVENUE";
        if (!Set.of("REVENUE", "TICKETS").contains(resolvedMetric)) {
            throw invalid("Invalid metric", "ANALYTICS_INVALID_METRIC");
        }
        int resolvedLimit = limit == null ? 10 : limit;
        if (resolvedLimit < 1 || resolvedLimit > 50) {
            throw invalid("Limit must be between 1 and 50", "ANALYTICS_INVALID_QUERY");
        }
        String resolvedDirection = StringUtils.hasText(direction) ? direction.toLowerCase() : "desc";
        if (!DIRECTIONS.contains(resolvedDirection)) {
            throw invalid("Invalid sort direction", "ANALYTICS_INVALID_SORT");
        }
        DateSelection dates = dates(startDate, endDate);
        String sortField = "REVENUE".equals(resolvedMetric) ? "totalRevenue" : "totalTicketsSold";
        List<MovieAggregate> aggregates = aggregate(load(dates)).stream()
                .sorted(comparator(sortField, resolvedDirection))
                .limit(resolvedLimit)
                .toList();
        List<TopMovieItemResponse> movies = new ArrayList<>();
        for (int index = 0; index < aggregates.size(); index++) {
            MovieAggregate value = aggregates.get(index);
            movies.add(new TopMovieItemResponse(
                    index + 1, value.movieId, value.movieTitle,
                    safeInt(value.ticketCount), math.money(value.revenue)));
        }
        Instant latest = aggregates.stream().map(value -> value.updatedAt)
                .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        return new TopMoviesResponse(
                resolvedMetric, dates.mode, dates.period(), CURRENCY, movies, utc(latest));
    }

    private List<MoviePerformanceDaily> load(DateSelection dates) {
        return dates.start == null
                ? repository.findAll()
                : repository.findAllByStatDateBetween(dates.start, dates.end);
    }

    private List<MovieAggregate> aggregate(List<MoviePerformanceDaily> values) {
        Map<String, MovieAggregate> aggregates = new LinkedHashMap<>();
        for (MoviePerformanceDaily value : values) {
            MovieAggregate aggregate = aggregates.computeIfAbsent(
                    value.getMovieKey(),
                    ignored -> new MovieAggregate(
                            value.getMovieId(), value.getMovieTitle(),
                            0, BigDecimal.ZERO, value.getUpdatedAt()));
            aggregate.ticketCount += value.getTicketCount();
            aggregate.revenue = aggregate.revenue.add(value.getNetRevenue());
            if (value.getUpdatedAt() != null
                    && (aggregate.updatedAt == null || value.getUpdatedAt().isAfter(aggregate.updatedAt))) {
                aggregate.updatedAt = value.getUpdatedAt();
            }
        }
        return new ArrayList<>(aggregates.values());
    }

    private Comparator<MovieAggregate> comparator(String field, String direction) {
        Comparator<MovieAggregate> comparator = switch (field) {
            case "movieTitle" -> Comparator.comparing(
                    value -> value.movieTitle, String.CASE_INSENSITIVE_ORDER);
            case "totalTicketsSold" -> Comparator.comparingLong(value -> value.ticketCount);
            default -> Comparator.comparing(value -> value.revenue);
        };
        comparator = comparator.thenComparing(value -> value.movieTitle, String.CASE_INSENSITIVE_ORDER);
        return "desc".equals(direction) ? comparator.reversed() : comparator;
    }

    private DateSelection dates(String start, String end) {
        boolean hasStart = StringUtils.hasText(start);
        boolean hasEnd = StringUtils.hasText(end);
        if (!hasStart && !hasEnd) {
            return new DateSelection("LIFETIME", null, null);
        }
        if (hasStart != hasEnd) {
            throw invalid("Both startDate and endDate must be provided together",
                    "ANALYTICS_INVALID_DATE_RANGE");
        }
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            if (startDate.isAfter(endDate) || ChronoUnit.DAYS.between(startDate, endDate) >= 92) {
                throw invalid("Analytics date range must not exceed 92 days",
                        "ANALYTICS_DATE_RANGE_TOO_LARGE");
            }
            return new DateSelection("DATE_RANGE", startDate, endDate);
        } catch (DateTimeParseException exception) {
            throw invalid("Invalid date format", "ANALYTICS_INVALID_DATE_FORMAT");
        }
    }

    private void requireMovieId(Long movieId) {
        if (movieId == null || movieId <= 0) {
            throw invalid("Movie ID must be greater than 0", "VALIDATION_ERROR");
        }
    }

    private int safeInt(long value) {
        return Math.toIntExact(value);
    }

    private LocalDateTime utc(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, ZoneOffset.UTC);
    }

    private BusinessException invalid(String message, String code) {
        return new BusinessException(message, code, HttpStatus.BAD_REQUEST);
    }

    private BusinessException notFound() {
        return new BusinessException(
                "Movie revenue statistics not found",
                "ANALYTICS_MOVIE_STATS_NOT_FOUND",
                HttpStatus.NOT_FOUND);
    }

    private record DateSelection(String mode, LocalDate start, LocalDate end) {
        AnalyticsPeriodResponse period() {
            return start == null ? null : new AnalyticsPeriodResponse(start.toString(), end.toString());
        }
    }

    private static final class MovieAggregate {
        private final Long movieId;
        private final String movieTitle;
        private long ticketCount;
        private BigDecimal revenue;
        private Instant updatedAt;

        private MovieAggregate(
                Long movieId,
                String movieTitle,
                long ticketCount,
                BigDecimal revenue,
                Instant updatedAt) {
            this.movieId = movieId;
            this.movieTitle = movieTitle;
            this.ticketCount = ticketCount;
            this.revenue = revenue;
            this.updatedAt = updatedAt;
        }
    }
}
