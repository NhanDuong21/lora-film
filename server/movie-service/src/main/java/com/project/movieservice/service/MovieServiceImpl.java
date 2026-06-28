package com.project.movieservice.service;

import com.project.movieservice.dto.*;
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
        
        if (search != null && search.length() > 255) {
            throw new BusinessException("Search query must not exceed 255 characters", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

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
                status = MovieStatus.valueOf(statusStr.trim().toUpperCase());
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

    @Override
    @Transactional(readOnly = true)
    public MoviePageResponse<AdminMovieListItemResponse> getAdminMovies(
            String pageStr, String sizeStr, String search, String statusStr, String genreIdStr,
            String releaseFromStr, String releaseToStr, String sortStr) {

        int page = 0;
        int size = 10;
        
        if (search != null && search.length() > 255) {
            throw new BusinessException("Search query must not exceed 255 characters", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }

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
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status parameter", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
        }

        Integer genreId = null;
        if (genreIdStr != null && !genreIdStr.trim().isEmpty()) {
            try {
                genreId = Integer.parseInt(genreIdStr);
                if (genreId <= 0) throw new NumberFormatException();
                
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
            
            if (!SORT_WHITELIST.contains(field) && !"createdAt".equals(field) && !"updatedAt".equals(field)) {
                throw new BusinessException("Invalid sort parameters", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
            }
            if (!direction.equals("asc") && !direction.equals("desc")) {
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
            query.distinct(true);
            
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            if (finalStatus != null) {
                predicates.add(cb.equal(root.get("status"), finalStatus));
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

        List<AdminMovieListItemResponse> content = moviePage.getContent().stream()
                .map(this::mapToAdminListItemResponse)
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
    public AdminMovieDetailResponse getAdminMovieDetail(String movieIdStr) {
        long movieId = parseMovieId(movieIdStr);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("Movie not found", "MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND));

        return mapToAdminDetailResponse(movie);
    }

    private void validateStatusAndDateConsistency(MovieStatus status, java.time.LocalDate releaseDate, java.time.LocalDate endDate) {
        java.time.LocalDate today = java.time.LocalDate.now();
        if (status == MovieStatus.UPCOMING) {
            if (today.isAfter(releaseDate)) {
                throw new BusinessException("UPCOMING movies cannot have a past release date", "MOVIE_INVALID_DATE_STATUS", HttpStatus.BAD_REQUEST);
            }
        } else if (status == MovieStatus.NOW_SHOWING) {
            if (today.isBefore(releaseDate) || today.isAfter(endDate)) {
                throw new BusinessException("NOW_SHOWING movies must be within their release period", "MOVIE_INVALID_DATE_STATUS", HttpStatus.BAD_REQUEST);
            }
        } else if (status == MovieStatus.ENDED) {
            if (today.isBefore(releaseDate)) {
                throw new BusinessException("ENDED movies cannot have a future release date", "MOVIE_INVALID_DATE_STATUS", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Override
    @Transactional
    public MovieCreatedResponse createMovie(MovieCreateRequest request) {
        if (request.getDurationMinutes() != null && request.getDurationMinutes() <= 0) {
            throw new BusinessException("Duration must be greater than 0", "MOVIE_INVALID_DURATION", HttpStatus.BAD_REQUEST);
        }

        if (request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BusinessException("Movie end date cannot be before release date", "MOVIE_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
        }

        MovieStatus newStatus;
        try {
            newStatus = MovieStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid movie status", "MOVIE_INVALID_STATUS", HttpStatus.BAD_REQUEST);
        }

        validateStatusAndDateConsistency(newStatus, request.getReleaseDate(), request.getEndDate());

        Movie movie = new Movie();
        mapRequestToMovie(request, movie);
        movie.setStatus(newStatus);

        List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
        if (genres.size() != request.getGenreIds().size()) {
            throw new BusinessException("One or more genres were not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        movie.setGenres(new java.util.HashSet<>(genres));

        Movie savedMovie = movieRepository.save(movie);
        return new MovieCreatedResponse(savedMovie.getId(), savedMovie.getTitle(), savedMovie.getStatus().name());
    }

    @Override
    @Transactional
    public MovieUpdatedResponse updateMovie(String movieIdStr, MovieUpdateRequest request) {
        long movieId = parseMovieId(movieIdStr);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("Movie not found", "MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (request.getDurationMinutes() != null && request.getDurationMinutes() <= 0) {
            throw new BusinessException("Duration must be greater than 0", "MOVIE_INVALID_DURATION", HttpStatus.BAD_REQUEST);
        }

        if (request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BusinessException("Movie end date cannot be before release date", "MOVIE_INVALID_DATE_RANGE", HttpStatus.BAD_REQUEST);
        }

        if (!movie.getDurationMinutes().equals(request.getDurationMinutes())) {
            // TODO: Dependency on Showtime issue. 
            // Currently blocking duration changes for active movies because we cannot safely check for future showtimes.
            if (movie.getStatus() != MovieStatus.UPCOMING) {
                throw new BusinessException("Movie duration cannot be changed because future showtimes already exist", "MOVIE_HAS_FUTURE_SHOWTIMES", HttpStatus.CONFLICT);
            }
        }

        MovieStatus newStatus;
        try {
            newStatus = MovieStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid movie status", "MOVIE_INVALID_STATUS", HttpStatus.BAD_REQUEST);
        }

        validateStatusAndDateConsistency(newStatus, request.getReleaseDate(), request.getEndDate());

        mapRequestToMovie(request, movie);
        movie.setStatus(newStatus);

        List<Genre> genres = genreRepository.findAllById(request.getGenreIds());
        if (genres.size() != request.getGenreIds().size()) {
            throw new BusinessException("One or more genres were not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        movie.setGenres(new java.util.HashSet<>(genres));

        Movie savedMovie = movieRepository.save(movie);
        return new MovieUpdatedResponse(savedMovie.getId(), savedMovie.getTitle(), savedMovie.getStatus().name());
    }

    @Override
    @Transactional
    public MovieStatusResponse updateMovieStatus(String movieIdStr, MovieStatusUpdateRequest request) {
        long movieId = parseMovieId(movieIdStr);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("Movie not found", "MOVIE_NOT_FOUND", HttpStatus.NOT_FOUND));

        MovieStatus currentStatus = movie.getStatus();
        MovieStatus newStatus;
        try {
            newStatus = MovieStatus.valueOf(request.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid movie status", "MOVIE_INVALID_STATUS", HttpStatus.BAD_REQUEST);
        }

        if (currentStatus == newStatus) {
            throw new BusinessException("Invalid movie status transition", "MOVIE_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        boolean validTransition = false;
        switch (currentStatus) {
            case UPCOMING:
                validTransition = (newStatus == MovieStatus.NOW_SHOWING || newStatus == MovieStatus.INACTIVE);
                break;
            case NOW_SHOWING:
                validTransition = (newStatus == MovieStatus.ENDED || newStatus == MovieStatus.INACTIVE);
                break;
            case ENDED:
                validTransition = (newStatus == MovieStatus.INACTIVE);
                break;
            case INACTIVE:
                validTransition = (newStatus == MovieStatus.UPCOMING);
                break;
        }

        if (!validTransition) {
            throw new BusinessException("Invalid movie status transition", "MOVIE_INVALID_STATUS_TRANSITION", HttpStatus.CONFLICT);
        }

        // Tái sử dụng method để đảm bảo logic và Error Code đồng nhất (MOVIE_INVALID_DATE_STATUS)
        validateStatusAndDateConsistency(newStatus, movie.getReleaseDate(), movie.getEndDate());

        movie.setStatus(newStatus);
        movieRepository.save(movie);
        return new MovieStatusResponse(movie.getId(), movie.getStatus().name());
    }

    private long parseMovieId(String movieIdStr) {
        try {
            long id = Long.parseLong(movieIdStr);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid movieId", "MOVIE_INVALID_QUERY", HttpStatus.BAD_REQUEST);
        }
    }

    private void mapRequestToMovie(MovieCreateRequest request, Movie movie) {
        movie.setTitle(request.getTitle().trim());
        movie.setDescription(request.getDescription());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setDirector(request.getDirector() != null ? request.getDirector().trim() : null);
        movie.setActor(request.getActor() != null ? request.getActor().trim() : null);
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setPosterUrl(request.getPosterUrl() != null ? request.getPosterUrl().trim() : null);
        movie.setTrailerUrl(request.getTrailerUrl() != null ? request.getTrailerUrl().trim() : null);
        
        if (request.getAgeRating() != null && !request.getAgeRating().trim().isEmpty()) {
            try {
                movie.setAgeRating(com.project.movieservice.enumtype.AgeRating.valueOf(request.getAgeRating().trim()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid age rating", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
            }
        } else {
            movie.setAgeRating(null);
        }
    }

    private AdminMovieListItemResponse mapToAdminListItemResponse(Movie movie) {
        AdminMovieListItemResponse dto = new AdminMovieListItemResponse();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setDurationMinutes(movie.getDurationMinutes());
        dto.setReleaseDate(movie.getReleaseDate());
        dto.setEndDate(movie.getEndDate());
        dto.setPosterUrl(movie.getPosterUrl());
        dto.setTrailerUrl(movie.getTrailerUrl());
        dto.setAgeRating(movie.getAgeRating() != null ? movie.getAgeRating().name() : null);
        dto.setStatus(movie.getStatus().name());
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());
        
        List<GenreSummaryResponse> genreResponses = movie.getGenres().stream()
                .map(g -> new GenreSummaryResponse(g.getId(), g.getGenreName()))
                .collect(Collectors.toList());
        dto.setGenres(genreResponses);
        return dto;
    }

    private AdminMovieDetailResponse mapToAdminDetailResponse(Movie movie) {
        AdminMovieDetailResponse dto = new AdminMovieDetailResponse();
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
        dto.setCreatedAt(movie.getCreatedAt());
        dto.setUpdatedAt(movie.getUpdatedAt());

        List<GenreSummaryResponse> genreResponses = movie.getGenres().stream()
                .map(g -> new GenreSummaryResponse(g.getId(), g.getGenreName()))
                .collect(Collectors.toList());
        dto.setGenres(genreResponses);
        return dto;
    }
}
