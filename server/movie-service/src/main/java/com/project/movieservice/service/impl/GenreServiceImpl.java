package com.project.movieservice.service.impl;

import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;
import com.project.movieservice.entity.Genre;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.GenreRepository;
import com.project.movieservice.service.GenreService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;

    public GenreServiceImpl(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GenreResponse> getGenres() {
        return genreRepository.findAllByOrderByGenreNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));
        return mapToResponse(genre);
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreCreateRequest request) {
        String normalizedName = normalizeName(request.getGenreName());

        if (genreRepository.existsByGenreNameIgnoreCase(normalizedName)) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        Genre genre = new Genre();
        genre.setGenreName(normalizedName);

        try {
            Genre savedGenre = genreRepository.save(genre);
            return mapToResponse(savedGenre);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(Integer genreId, GenreUpdateRequest request) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new BusinessException("Genre not found", "GENRE_NOT_FOUND", HttpStatus.NOT_FOUND));

        String normalizedName = normalizeName(request.getGenreName());

        if (genreRepository.existsByGenreNameIgnoreCaseAndIdNot(normalizedName, genreId)) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        genre.setGenreName(normalizedName);

        try {
            Genre updatedGenre = genreRepository.save(genre);
            return mapToResponse(updatedGenre);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Genre already exists", "GENRE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
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
        return new GenreResponse(genre.getId(), genre.getGenreName());
    }
}
