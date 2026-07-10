package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMediaRepository movieMediaRepository;
    private final MovieMapper movieMapper;

    public MovieServiceImpl(MovieRepository movieRepository,
                            MovieGenreRepository movieGenreRepository,
                            MovieMediaRepository movieMediaRepository,
                            MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMediaRepository = movieMediaRepository;
        this.movieMapper = movieMapper;
    }

    @Override
    public PageResponse<MovieDto> getMovies(String status, String keyword, int page, int size, String sort) {
        Specification<Movie> spec = Specification.where(MovieSpecification.isNotDeleted());

        if (status != null && !status.isEmpty()) {
            MovieStatus parsedStatus = MovieStatus.fromString(status);
            if (parsedStatus == MovieStatus.NOW_SHOWING || parsedStatus == MovieStatus.UPCOMING) {
                spec = spec.and(MovieSpecification.hasStatus(parsedStatus));
            } else {
                spec = spec.and(MovieSpecification.isPubliclyVisible());
            }
        } else {
            spec = spec.and(MovieSpecification.isPubliclyVisible());
        }

        if (keyword != null && !keyword.isEmpty()) {
            spec = spec.and(MovieSpecification.hasKeyword(keyword));
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("releaseDate").descending());
        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        List<MovieDto> movieDtos = moviePage.getContent().stream().map(this::mapToDto).collect(Collectors.toList());

        return new PageResponse<>(
                movieDtos,
                moviePage.getNumber(),
                moviePage.getSize(),
                moviePage.getTotalElements(),
                moviePage.getTotalPages(),
                moviePage.isLast()
        );
    }

    @Override
    public MovieDto getMovieBySlug(String slug) {
        Movie movie = movieRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        if (movie.getStatus() == MovieStatus.DRAFT || movie.getStatus() == MovieStatus.INACTIVE) {
            throw new ResourceNotFoundException("Movie not found");
        }

        return mapToDto(movie);
    }

    private MovieDto mapToDto(Movie movie) {
        List<String> genres = movieGenreRepository.findByMovieId(movie.getId())
                .stream()
                .map(mg -> mg.getGenre().getName())
                .collect(Collectors.toList());

        Optional<MovieMedia> primaryPoster = movieMediaRepository
                .findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        movie.getId(), MovieMediaType.POSTER, ActiveStatus.ACTIVE);

        String posterUrl = primaryPoster.map(MovieMedia::getUrl).orElse(null);

        return movieMapper.toDto(movie, genres, posterUrl);
    }
}
