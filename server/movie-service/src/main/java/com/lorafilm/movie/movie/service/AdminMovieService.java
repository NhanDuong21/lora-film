package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.dto.MovieRequest;
import com.lorafilm.movie.movie.repository.GenreRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final MovieMapper movieMapper;

    public AdminMovieService(MovieRepository movieRepository, GenreRepository genreRepository, MovieGenreRepository movieGenreRepository, MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieGenreRepository = movieGenreRepository;
        this.movieMapper = movieMapper;
    }

    @Transactional
    public MovieDto createMovie(MovieRequest request) {
        validateMovieDates(request);
        
        String slug = generateUniqueSlug(request.getTitle());

        Movie movie = new Movie();
        movie.setPublicId(UUID.randomUUID().toString());
        movie.setTitle(request.getTitle());
        movie.setOriginalTitle(request.getOriginalTitle());
        movie.setSynopsis(request.getSynopsis());
        movie.setSlug(slug);
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setCountry(request.getCountry());
        movie.setStatus(MovieStatus.DRAFT);
        
        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    @Transactional
    public MovieDto updateMovie(String publicId, MovieRequest request) {
        validateMovieDates(request);

        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));

        if (!movie.getTitle().equals(request.getTitle())) {
            movie.setSlug(generateUniqueSlug(request.getTitle()));
        }

        movie.setTitle(request.getTitle());
        movie.setOriginalTitle(request.getOriginalTitle());
        movie.setSynopsis(request.getSynopsis());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setAgeRating(request.getAgeRating());
        movie.setReleaseDate(request.getReleaseDate());
        movie.setEndDate(request.getEndDate());
        movie.setCountry(request.getCountry());
        
        if (request.getStatus() != null && request.getStatus() != movie.getStatus()) {
            validatePublishStatus(movie.getId(), request.getStatus());
            movie.setStatus(request.getStatus());
        }

        Movie saved = movieRepository.save(movie);
        return mapToDto(saved);
    }

    @Transactional
    public void assignGenres(String moviePublicId, List<String> genreIds) {
        Movie movie = movieRepository.findByPublicIdAndDeletedAtIsNull(moviePublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found", null));
        
        if ((movie.getStatus() == MovieStatus.UPCOMING || movie.getStatus() == MovieStatus.NOW_SHOWING) 
            && (genreIds == null || genreIds.isEmpty())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Published movie must have at least one genre", null);
        }

        List<Genre> genres = genreRepository.findByPublicIdInAndDeletedAtIsNull(genreIds);
        if (genres.size() != (genreIds == null ? 0 : genreIds.size())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "One or more genres do not exist", null);
        }

        movieGenreRepository.deleteByMovieId(movie.getId());
        
        for (Genre genre : genres) {
            MovieGenre movieGenre = new MovieGenre();
            movieGenre.setMovie(movie);
            movieGenre.setGenre(genre);
            movieGenreRepository.save(movieGenre);
        }
    }

    private void validateMovieDates(MovieRequest request) {
        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getReleaseDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "End date cannot be before release date", null);
        }
    }

    private void validatePublishStatus(Long movieId, MovieStatus newStatus) {
        if (newStatus == MovieStatus.UPCOMING || newStatus == MovieStatus.NOW_SHOWING) {
            List<MovieGenre> genres = movieGenreRepository.findByMovieId(movieId);
            if (genres.isEmpty()) {
                throw new BusinessException(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, "Movie must have at least 1 genre to be published", null);
            }
        }
    }

    private String generateUniqueSlug(String title) {
        if (title == null) return "";
        String normalized = Normalizer.normalize(title, Normalizer.Form.NFD);
        String baseSlug = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        
        List<String> existingSlugs = movieRepository.findActiveSlugsByPrefix(baseSlug);
        if (existingSlugs.isEmpty() || !existingSlugs.contains(baseSlug)) {
            return baseSlug;
        }

        int maxSuffix = 0;
        for (String activeSlug : existingSlugs) {
            if (activeSlug.startsWith(baseSlug + "-")) {
                try {
                    String suffixStr = activeSlug.substring(baseSlug.length() + 1);
                    int suffix = Integer.parseInt(suffixStr);
                    maxSuffix = Math.max(maxSuffix, suffix);
                } catch (NumberFormatException ignored) {}
            }
        }
        return baseSlug + "-" + (maxSuffix + 1);
    }
    
    private MovieDto mapToDto(Movie movie) {
        List<MovieGenre> movieGenres = movieGenreRepository.findByMovieId(movie.getId());
        List<String> genreNames = movieGenres.stream().map(mg -> mg.getGenre().getName()).collect(Collectors.toList());
        return movieMapper.toDto(movie, genreNames, null);
    }
}
