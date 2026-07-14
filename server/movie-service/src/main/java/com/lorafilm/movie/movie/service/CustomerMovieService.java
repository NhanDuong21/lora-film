package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerMovieService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMapper movieMapper;
    private final MovieService movieService;

    public CustomerMovieService(MovieRepository movieRepository, MovieGenreRepository movieGenreRepository, MovieMapper movieMapper, MovieService movieService) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMapper = movieMapper;
        this.movieService = movieService;
    }

    public PageResponse<MovieDto> getMoviesByStatus(String statusStr, String keyword, Pageable pageable) {
        MovieStatus status;
        if ("now-showing".equalsIgnoreCase(statusStr)) {
            status = MovieStatus.NOW_SHOWING;
        } else if ("coming-soon".equalsIgnoreCase(statusStr)) {
            status = MovieStatus.UPCOMING;
        } else {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Invalid status query. Must be now-showing or coming-soon.", null);
        }

        Specification<Movie> spec = Specification.where(com.lorafilm.movie.movie.repository.MovieSpecification.isNotDeleted())
                .and(com.lorafilm.movie.movie.repository.MovieSpecification.hasStatus(status));
                
        if (keyword != null && !keyword.trim().isEmpty()) {
            spec = spec.and(com.lorafilm.movie.movie.repository.MovieSpecification.hasKeyword(keyword.trim()));
        }

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);
        
        List<MovieDto> content = moviePage.getContent().stream()
                .map(this::mapToDto)
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

    private MovieDto mapToDto(Movie movie) {
        List<MovieGenre> movieGenres = movieGenreRepository.findByMovieId(movie.getId());
        List<String> genreNames = movieGenres.stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        return movieMapper.toDto(movie, genreNames, null);
    }
}
