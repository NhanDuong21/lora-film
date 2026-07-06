package com.project.movieservice.service.impl;

import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;
import com.project.movieservice.entity.Genre;
import com.project.movieservice.enumtype.GenreStatus;
import com.project.movieservice.enumtype.MovieStatus;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.GenreRepository;
import com.project.movieservice.repository.MovieRepository;
import com.project.movieservice.service.GenreService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final MovieRepository movieRepository;

    public GenreServiceImpl(GenreRepository genreRepository, MovieRepository movieRepository) {
        this.genreRepository = genreRepository;
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "genres", key = "#isAdmin")
    public List<GenreResponse> getGenres(boolean isAdmin) {
        List<Genre> genres;
        if (isAdmin) {
            genres = genreRepository.findAllByOrderByGenreNameAsc();
        } else {
            genres = genreRepository.findByStatusOrderByGenreNameAsc(GenreStatus.ACTIVE);
        }
        return genres.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "genreDetail", key = "{#genreId, #isAdmin}")
    public GenreResponse getGenreById(Integer genreId, boolean isAdmin) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));
        
        if (!isAdmin && genre.getStatus() != GenreStatus.ACTIVE) {
            throw new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        return mapToResponse(genre);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"genres", "genreDetail"}, allEntries = true)
    public GenreResponse createGenre(GenreCreateRequest request) {
        String normalizedName = normalizeName(request.getGenreName());

        if (genreRepository.existsByGenreNameIgnoreCase(normalizedName)) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        Genre genre = new Genre();
        genre.setGenreName(normalizedName);
        genre.setStatus(GenreStatus.ACTIVE);

        try {
            Genre savedGenre = genreRepository.save(genre);
            return mapToResponse(savedGenre);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"genres", "genreDetail", "movies", "movieDetail"}, allEntries = true)
    public GenreResponse updateGenre(Integer genreId, GenreUpdateRequest request) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));

        String normalizedName = normalizeName(request.getGenreName());

        if (genreRepository.existsByGenreNameIgnoreCaseAndIdNot(normalizedName, genreId)) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        GenreStatus newStatus;
        try {
            newStatus = GenreStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid genre status", "GENRE_INVALID_STATUS", HttpStatus.BAD_REQUEST);
        }

        if (genre.getStatus() != newStatus) {
            if (newStatus == GenreStatus.INACTIVE) {
                boolean isInUse = movieRepository.existsByGenresIdAndStatusIn(genreId, Arrays.asList(MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING, MovieStatus.ENDED));
                if (isInUse) {
                    throw new BusinessException("Cannot inactive genre currently used by public movies", "GENRE_IN_USE", HttpStatus.CONFLICT);
                }
            }
        }

        genre.setGenreName(normalizedName);
        genre.setStatus(newStatus);

        try {
            Genre updatedGenre = genreRepository.save(genre);
            return mapToResponse(updatedGenre);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {"genres", "genreDetail", "movies", "movieDetail"}, allEntries = true)
    public void softDeleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (genre.getStatus() == GenreStatus.INACTIVE) {
            return;
        }

        boolean isInUse = movieRepository.existsByGenresIdAndStatusIn(genreId, Arrays.asList(MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING, MovieStatus.ENDED));
        if (isInUse) {
            throw new BusinessException("Cannot delete genre currently used by public movies", "GENRE_IN_USE", HttpStatus.CONFLICT);
        }

        genre.setStatus(GenreStatus.INACTIVE);
        genreRepository.save(genre);
    }

    private String normalizeName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("Genre name cannot be empty or blank", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("Genre name cannot be empty", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (trimmed.length() > 100) {
            throw new BusinessException("Genre name must not exceed 100 characters", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        if (!trimmed.matches("^(?=.*[a-zA-ZÀ-ỹ])[a-zA-ZÀ-ỹ0-9\\s\\-&]+$")) {
            throw new BusinessException("Genre name must contain at least one letter and can only include letters, numbers, spaces, hyphens, and ampersands", "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        return trimmed;
    }

    private GenreResponse mapToResponse(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getGenreName(), genre.getStatus().name());
    }
}
