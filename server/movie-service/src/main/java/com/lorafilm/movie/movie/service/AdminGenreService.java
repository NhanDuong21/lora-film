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
    private final com.lorafilm.movie.movie.repository.MovieGenreRepository movieGenreRepository;

    public AdminGenreService(GenreRepository genreRepository, GenreMapper genreMapper, com.lorafilm.movie.movie.repository.MovieGenreRepository movieGenreRepository) {
        this.genreRepository = genreRepository;
        this.genreMapper = genreMapper;
        this.movieGenreRepository = movieGenreRepository;
    }

    public com.lorafilm.movie.common.api.PageResponse<GenreResponse> getGenres(int page, int size) {
        org.springframework.data.domain.Page<Genre> genrePage = genreRepository.findByDeletedAtIsNull(org.springframework.data.domain.PageRequest.of(page - 1, size));
        return com.lorafilm.movie.common.api.PageResponse.of(genrePage, genrePage.getContent().stream().map(genreMapper::toResponse).toList());
    }

    public GenreResponse getGenre(String publicId) {
        Genre genre = genreRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Genre not found", null));
        return genreMapper.toResponse(genre);
    }

    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        String baseSlug = generateSlug(request.getName());
        if (genreRepository.existsBySlugAndDeletedAtIsNull(baseSlug)) {
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
        if (!newSlug.equals(genre.getSlug()) && genreRepository.existsBySlugAndDeletedAtIsNull(newSlug)) {
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
    
    @Transactional
    public void deleteGenre(String publicId) {
        Genre genre = genreRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Genre not found", null));
        
        if (movieGenreRepository.existsByGenreIdAndMovieDeletedAtIsNull(genre.getId())) {
            throw new BusinessException(ErrorCode.GENRE_IN_USE, "Cannot delete genre because it is currently used by one or more active movies", null);
        }
        
        Long userId = 1L;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            try {
                userId = Long.valueOf(auth.getName());
            } catch (Exception e) {}
        }
        
        genre.setStatus(com.lorafilm.movie.common.enums.ActiveStatus.INACTIVE);
        genre.performSoftDelete(userId);
        genreRepository.save(genre);
    }
    
    private String generateSlug(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                                   .replaceAll("đ", "d").replaceAll("Đ", "D");
        return noAccents.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}
