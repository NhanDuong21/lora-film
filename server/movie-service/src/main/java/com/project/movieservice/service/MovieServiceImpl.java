package com.project.movieservice.service;

import com.project.movieservice.dto.GenreSummaryResponse;
import com.project.movieservice.dto.MovieDetailResponse;
import com.project.movieservice.dto.MovieListItemResponse;
import com.project.movieservice.dto.MoviePageResponse;
import com.project.movieservice.entity.Genre;
import com.project.movieservice.entity.Movie;
import com.project.movieservice.enumtype.MovieStatus;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.GenreRepository;
import com.project.movieservice.repository.MovieRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    private static final List<MovieStatus> PUBLIC_STATUSES = Arrays.asList(
            MovieStatus.UPCOMING,
            MovieStatus.NOW_SHOWING,
            MovieStatus.ENDED
    );

    private static final List<String> SORT_WHITELIST = Arrays.asList(
            "id", "title", "durationMinutes", "releaseDate", "endDate", "status", "ageRating"
    );

    public MovieServiceImpl(MovieRepository movieRepository, GenreRepository genreRepository) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MoviePageResponse<MovieListItemResponse> getMovies(
            String pageStr, String sizeStr, String search, String statusStr, String genreIdStr,
            String releaseFromStr, String releaseToStr, String sortStr) {

        int page = 0;
        int size = 10;
        
        if (pageStr != null) {
            try {
                page = Integer.parseInt(pageStr);
                if (page < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid page parameter", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }
        
        if (sizeStr != null) {
            try {
                size = Integer.parseInt(sizeStr);
                if (size < 1 || size > 50) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                throw new BusinessException("Size must be between 1 and 50", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }

        MovieStatus status = null;
        if (statusStr != null && !statusStr.trim().isEmpty()) {
            try {
                status = MovieStatus.valueOf(statusStr);
                if (!PUBLIC_STATUSES.contains(status)) {
                    // Even if client asks for INACTIVE, return empty based on rule or bad request if invalid
                    // Wait, instruction says: "Không được hiển thị: INACTIVE. Kể cả khi client gọi ?status=INACTIVE (trả danh sách rỗng)."
                    if (status == MovieStatus.INACTIVE) {
                        return new MoviePageResponse<>(new ArrayList<>(), page, size, 0, 0, true, true);
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status parameter", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }

        Integer genreId = null;
        if (genreIdStr != null && !genreIdStr.trim().isEmpty()) {
            try {
                genreId = Integer.parseInt(genreIdStr);
                if (genreId <= 0) throw new NumberFormatException();
                
                // Validate genre existence
                if (!genreRepository.existsById(genreId)) {
                    throw new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND);
                }
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid genreId parameter", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }

        LocalDate releaseFrom = null;
        LocalDate releaseTo = null;
        if (releaseFromStr != null && !releaseFromStr.trim().isEmpty()) {
            try {
                releaseFrom = LocalDate.parse(releaseFromStr);
            } catch (DateTimeParseException e) {
                throw new BusinessException("Invalid releaseFrom format", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }
        if (releaseToStr != null && !releaseToStr.trim().isEmpty()) {
            try {
                releaseTo = LocalDate.parse(releaseToStr);
            } catch (DateTimeParseException e) {
                throw new BusinessException("Invalid releaseTo format", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }
        
        if (releaseFrom != null && releaseTo != null && releaseFrom.isAfter(releaseTo)) {
            throw new BusinessException("releaseFrom cannot be after releaseTo", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        Sort sort = Sort.unsorted();
        if (sortStr != null && !sortStr.trim().isEmpty()) {
            String[] parts = sortStr.split(",");
            if (parts.length != 2) {
                throw new BusinessException("Invalid sort format", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
            String field = parts[0];
            String direction = parts[1].toLowerCase();
            
            if (!SORT_WHITELIST.contains(field) || (!direction.equals("asc") && !direction.equals("desc"))) {
                throw new BusinessException("Invalid sort parameters", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
            sort = Sort.by(Sort.Direction.fromString(direction), field);
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        
        final MovieStatus finalStatus = status;
        final Integer finalGenreId = genreId;
        final LocalDate finalReleaseFrom = releaseFrom;
        final LocalDate finalReleaseTo = releaseTo;
        final String finalSearch = (search != null && !search.trim().isEmpty()) ? search.trim().toLowerCase() : null;

        Specification<Movie> spec = (root, query, cb) -> {
            // Needed to avoid duplicate records when fetching collection
            query.distinct(true);
            
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            // Public status filter
            if (finalStatus != null) {
                predicates.add(cb.equal(root.get("status"), finalStatus));
            } else {
                predicates.add(root.get("status").in(PUBLIC_STATUSES));
            }
            
            if (finalSearch != null) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + finalSearch + "%"));
            }
            
            if (finalGenreId != null) {
                Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);
                predicates.add(cb.equal(genreJoin.get("id"), finalGenreId));
            }
            
            if (finalReleaseFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("releaseDate"), finalReleaseFrom));
            }
            
            if (finalReleaseTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("releaseDate"), finalReleaseTo));
            }
            
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        List<MovieListItemResponse> content = moviePage.getContent().stream()
                .map(this::mapToListItemResponse)
                .collect(Collectors.toList());

        return new MoviePageResponse<>(
                content,
                moviePage.getNumber(),
                moviePage.getSize(),
                moviePage.getTotalElements(),
                moviePage.getTotalPages(),
                moviePage.isFirst(),
                moviePage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MovieDetailResponse getMovieDetail(String movieIdStr) {
        long movieId;
        try {
            movieId = Long.parseLong(movieIdStr);
            if (movieId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid movieId", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("Movie not found", "MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!PUBLIC_STATUSES.contains(movie.getStatus())) {
            throw new BusinessException("Movie not found", "MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }

        return mapToDetailResponse(movie);
    }

    private MovieListItemResponse mapToListItemResponse(Movie movie) {
        MovieListItemResponse dto = new MovieListItemResponse();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setEndDate(movie.getEndDate());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setAgeRating(movie.getAgeRating() != null ? movie.getAgeRating().name() : null);
        dto.setStatus(movie.getStatus().name());
        
        List<GenreSummaryResponse> genreResponses = movie.getGenres().stream()
                .map(g -> new GenreSummaryResponse(g.getId(), g.getGenreName()))
                .collect(Collectors.toList());
        dto.setGenres(genreResponses);
        return dto;
    }

    private MovieDetailResponse mapToDetailResponse(Movie movie) {
        MovieDetailResponse dto = new MovieDetailResponse();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDescription(movie.getDescription());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setDirector(movie.getDirector());
        dto.setActor(movie.getActor());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setEndDate(movie.getEndDate());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setAgeRating(movie.getAgeRating() != null ? movie.getAgeRating().name() : null);
        dto.setStatus(movie.getStatus().name());

        List<GenreSummaryResponse> genreResponses = movie.getGenres().stream()
                .map(g -> new GenreSummaryResponse(g.getId(), g.getGenreName()))
                .collect(Collectors.toList());
        dto.setGenres(genreResponses);
        return dto;
    }
}
