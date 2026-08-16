package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.util.SeatPriceAllocation;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerMovieService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieMapper movieMapper;
    private final MovieService movieService;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository priceRepository;
    private final Clock clock;

    public CustomerMovieService(MovieRepository movieRepository,
                                MovieGenreRepository movieGenreRepository,
                                MovieMediaRepository movieMediaRepository,
                                MovieMapper movieMapper,
                                MovieService movieService,
                                ShowtimeRepository showtimeRepository,
                                ShowtimePriceRepository priceRepository,
                                Clock clock) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieMapper = movieMapper;
        this.movieService = movieService;
        this.showtimeRepository = showtimeRepository;
        this.priceRepository = priceRepository;
        this.clock = clock;
    }

    public PageResponse<MovieDto> getMoviesByStatus(String statusStr, String keyword, Pageable pageable) {
        return getMoviesByStatus(statusStr, keyword, null, pageable);
    }

    public PageResponse<MovieDto> getMoviesByStatus(
            String statusStr,
            String keyword,
            String genrePublicId,
            Pageable pageable) {
        Specification<Movie> spec = Specification.where(
                com.lorafilm.movie.movie.repository.MovieSpecification.isNotDeleted());
        LocalDate today = LocalDate.now(clock);

        if (statusStr == null || statusStr.isBlank() || "all".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.isCustomerCatalogVisible(today));
        } else if ("now-showing".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.hasStatus(
                            MovieStatus.NOW_SHOWING));
        } else if ("coming-soon".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.isFutureUpcoming(today));
        } else {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Trạng thái lọc không hợp lệ. Chỉ chấp nhận tất cả, đang chiếu hoặc sắp chiếu.",
                    null);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(com.lorafilm.movie.movie.repository.MovieSpecification.hasKeyword(keyword.trim()));
        }
        if (genrePublicId != null && !genrePublicId.isBlank()) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.hasGenrePublicId(
                            genrePublicId.trim()));
        }

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);
        
        List<Long> movieIds = moviePage.getContent().stream().map(Movie::getId).toList();
        Map<Long, String> primaryPosters = movieIds.isEmpty()
                ? Map.of()
                : movieMediaRepository
                        .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                                movieIds,
                                MovieMediaType.POSTER,
                                ActiveStatus.ACTIVE)
                        .stream()
                        .collect(Collectors.toMap(
                                media -> media.getMovie().getId(),
                                media -> media.getUrl(),
                                (first, ignored) -> first));
        Map<Long, String> primaryTrailers = movieIds.isEmpty()
                ? Map.of()
                : movieMediaRepository
                        .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                                movieIds,
                                MovieMediaType.TRAILER,
                                ActiveStatus.ACTIVE)
                        .stream()
                        .collect(Collectors.toMap(
                                media -> media.getMovie().getId(),
                                media -> media.getUrl(),
                                (first, ignored) -> first));

        Map<Long, Availability> availability = loadAvailability(movieIds);
        List<MovieDto> content = moviePage.getContent().stream()
                .map(movie -> {
                    MovieDto dto = mapToDto(movie, primaryPosters.get(movie.getId()));
                    dto.setTrailerUrl(primaryTrailers.get(movie.getId()));
                    applyAvailability(dto, availability.get(movie.getId()));
                    return dto;
                })
                .collect(Collectors.toList());

        return PageResponse.of(moviePage, content);
    }

    public com.lorafilm.movie.movie.dto.MovieDetailDto getMovieDetail(String identifier) {
        Movie movie = movieRepository.findByIdentifierAndDeletedAtIsNull(identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Không tìm thấy phim.", null));
        
        boolean normallyVisible = movie.getStatus() == MovieStatus.NOW_SHOWING
                || movie.getStatus() == MovieStatus.ENDED
                || (movie.getStatus() == MovieStatus.UPCOMING
                    && movie.getReleaseDate() != null
                    && movie.getReleaseDate().isAfter(LocalDate.now(clock)));
        if (!normallyVisible && !showtimeRepository
                .existsByMovieIdAndStatusAndStartTimeAfterAndDeletedAtIsNull(
                        movie.getId(), ShowtimeStatus.OPEN_FOR_BOOKING, Instant.now(clock))) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Không tìm thấy phim.", null);
        }

        var detail = movieService.getMovieByIdentifier(movie.getPublicId());
        applyAvailability(detail, loadAvailability(List.of(movie.getId())).get(movie.getId()));
        detail.setCatalogVisible(normallyVisible);
        return detail;
    }

    private MovieDto mapToDto(Movie movie, String primaryPosterUrl) {
        List<MovieGenre> movieGenres = movieGenreRepository.findByMovieId(movie.getId());
        List<String> genreNames = movieGenres.stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        return movieMapper.toDto(movie, genreNames, primaryPosterUrl);
    }

    private Map<Long, Availability> loadAvailability(Collection<Long> movieIds) {
        if (movieIds.isEmpty()) {
            return Map.of();
        }
        List<Showtime> showtimes = showtimeRepository.findCustomerAvailableByMovieIds(
                movieIds, Instant.now(clock));
        if (showtimes.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<ShowtimePrice>> pricesByShowtime = priceRepository
                .findByShowtimeIdInWithSeatType(showtimes.stream().map(Showtime::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(price -> price.getShowtime().getId()));

        Map<Long, AvailabilityAccumulator> accumulators = new HashMap<>();
        for (Showtime showtime : showtimes) {
            AvailabilityAccumulator accumulator = accumulators.computeIfAbsent(
                    showtime.getMovie().getId(), ignored -> new AvailabilityAccumulator());
            accumulator.count++;
            if (accumulator.nextShowtimeAt == null
                    || showtime.getStartTime().isBefore(accumulator.nextShowtimeAt)) {
                accumulator.nextShowtimeAt = showtime.getStartTime();
            }
            for (ShowtimePrice price : pricesByShowtime.getOrDefault(showtime.getId(), List.of())) {
                if (price.getPrice() == null || price.getPrice().signum() <= 0) continue;
                BigDecimal allocated = SeatPriceAllocation.perPhysicalSeat(
                        price.getSeatType().getCode(), price.getPrice());
                if (accumulator.priceFrom == null || allocated.compareTo(accumulator.priceFrom) < 0) {
                    accumulator.priceFrom = allocated;
                    accumulator.currency = price.getCurrency();
                }
            }
        }
        return accumulators.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().toValue()));
    }

    private void applyAvailability(MovieDto dto, Availability availability) {
        Availability value = availability == null
                ? new Availability(0, null, null, null)
                : availability;
        dto.setCatalogVisible(true);
        dto.setBookable(value.count() > 0);
        dto.setBookableShowtimeCount(value.count());
        dto.setNextShowtimeAt(value.nextShowtimeAt());
        dto.setPriceFrom(value.priceFrom());
        dto.setCurrency(value.currency());
    }

    private record Availability(long count, Instant nextShowtimeAt,
                                BigDecimal priceFrom, String currency) {}

    private static final class AvailabilityAccumulator {
        private long count;
        private Instant nextShowtimeAt;
        private BigDecimal priceFrom;
        private String currency;

        private Availability toValue() {
            return new Availability(count, nextShowtimeAt, priceFrom, currency);
        }
    }
}
