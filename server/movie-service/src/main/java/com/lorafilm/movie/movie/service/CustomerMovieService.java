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
import com.lorafilm.movie.common.enums.ActiveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public CustomerMovieService(MovieRepository movieRepository,
                                MovieGenreRepository movieGenreRepository,
                                MovieMediaRepository movieMediaRepository,
                                MovieMapper movieMapper,
                                MovieService movieService) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieMapper = movieMapper;
        this.movieService = movieService;
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

        if (statusStr == null || statusStr.isBlank() || "all".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.isPubliclyVisible());
        } else if ("now-showing".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.hasStatus(
                            MovieStatus.NOW_SHOWING));
        } else if ("coming-soon".equalsIgnoreCase(statusStr)) {
            spec = spec.and(
                    com.lorafilm.movie.movie.repository.MovieSpecification.hasStatus(
                            MovieStatus.UPCOMING));
        } else {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Invalid status query. Must be all, now-showing or coming-soon.",
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

        List<MovieDto> content = moviePage.getContent().stream()
                .map(movie -> mapToDto(movie, primaryPosters.get(movie.getId())))
                .collect(Collectors.toList());

        return PageResponse.of(moviePage, content);
    }

    public com.lorafilm.movie.movie.dto.MovieDetailDto getMovieDetail(String identifier) {
        Movie movie = movieRepository.findByIdentifierAndDeletedAtIsNull(identifier)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if (movie.getStatus() == MovieStatus.DRAFT || movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null);
        }

        return movieService.getMovieByIdentifier(movie.getPublicId());
    }

    private MovieDto mapToDto(Movie movie, String primaryPosterUrl) {
        List<MovieGenre> movieGenres = movieGenreRepository.findByMovieId(movie.getId());
        List<String> genreNames = movieGenres.stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        return movieMapper.toDto(movie, genreNames, primaryPosterUrl);
    }
}
