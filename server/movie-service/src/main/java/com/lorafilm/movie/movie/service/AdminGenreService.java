package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.dto.GenreMapper;
import com.lorafilm.movie.movie.dto.GenreRequest;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.repository.GenreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;

@Service
public class AdminGenreService {
    
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public AdminGenreService(GenreRepository genreRepository, GenreMapper genreMapper) {
        this.genreRepository = genreRepository;
        this.genreMapper = genreMapper;
    }

    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        String baseSlug = generateSlug(request.getName());
        if (genreRepository.existsByActiveSlugAndDeletedAtIsNull(baseSlug)) {
            throw new BusinessException(ErrorCode.GENRE_DUPLICATED, "Genre already exists.", null);
        }

        Genre genre = new Genre();
        genre.setPublicId(UUID.randomUUID().toString());
        genre.setName(request.getName());
        genre.setSlug(baseSlug);
        if (request.getStatus() != null) {
            genre.setStatus(request.getStatus());
        }
        
        Genre saved = genreRepository.save(genre);
        return genreMapper.toResponse(saved);
    }

    @Transactional
    public GenreResponse updateGenre(String publicId, GenreRequest request) {
        Genre genre = genreRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Genre not found", null));
        
        String newSlug = generateSlug(request.getName());
        if (!newSlug.equals(genre.getSlug()) && genreRepository.existsByActiveSlugAndDeletedAtIsNull(newSlug)) {
            throw new BusinessException(ErrorCode.GENRE_DUPLICATED, "Genre already exists.", null);
        }

        genre.setName(request.getName());
        genre.setSlug(newSlug);
        if (request.getStatus() != null) {
            genre.setStatus(request.getStatus());
        }

        Genre saved = genreRepository.save(genre);
        return genreMapper.toResponse(saved);
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccents.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-");
    }
}
