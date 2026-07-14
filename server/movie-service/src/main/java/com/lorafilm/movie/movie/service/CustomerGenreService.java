package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.dto.GenreMapper;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.repository.GenreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerGenreService {
    
    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    public CustomerGenreService(GenreRepository genreRepository, GenreMapper genreMapper) {
        this.genreRepository = genreRepository;
        this.genreMapper = genreMapper;
    }

    public List<GenreResponse> getActiveGenres() {
        return genreRepository.findByStatusAndDeletedAtIsNull(ActiveStatus.ACTIVE)
                .stream()
                .map(genreMapper::toResponse)
                .collect(Collectors.toList());
    }
}
