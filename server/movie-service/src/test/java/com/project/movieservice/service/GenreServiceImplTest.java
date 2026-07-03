package com.project.movieservice.service;

import com.project.movieservice.dto.GenreCreateRequest;
import com.project.movieservice.dto.GenreResponse;
import com.project.movieservice.dto.GenreUpdateRequest;
import com.project.movieservice.entity.Genre;
import com.project.movieservice.exception.BusinessException;
import com.project.movieservice.repository.GenreRepository;
import com.project.movieservice.service.impl.GenreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreServiceImpl genreService;

    private Genre genre;

    @BeforeEach
    void setUp() {
        genre = new Genre(1, "Action");
    }

    @Test
    void getGenres_ShouldReturnList() {
        when(genreRepository.findAllByOrderByGenreNameAsc()).thenReturn(Collections.singletonList(genre));
        List<GenreResponse> responses = genreService.getGenres();
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getGenreName()).isEqualTo("Action");
    }

    @Test
    void getGenreById_ShouldReturnGenre_WhenExists() {
        when(genreRepository.findById(1)).thenReturn(Optional.of(genre));
        GenreResponse response = genreService.getGenreById(1);
        assertThat(response.getGenreName()).isEqualTo("Action");
    }

    @Test
    void getGenreById_ShouldThrowNotFound_WhenNotExists() {
        when(genreRepository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> genreService.getGenreById(1))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Genre not found");
    }

    @Test
    void createGenre_ShouldReturnGenre_WhenValidAndNotExists() {
        GenreCreateRequest request = new GenreCreateRequest(" Action ");
        when(genreRepository.existsByGenreNameIgnoreCase("Action")).thenReturn(false);
        when(genreRepository.save(any())).thenAnswer(i -> {
            Genre g = i.getArgument(0);
            g.setId(2);
            return g;
        });

        GenreResponse response = genreService.createGenre(request);
        assertThat(response.getGenreName()).isEqualTo("Action");
        assertThat(response.getId()).isEqualTo(2);
    }

    @Test
    void createGenre_ShouldThrowConflict_WhenDuplicate() {
        GenreCreateRequest request = new GenreCreateRequest("Action");
        when(genreRepository.existsByGenreNameIgnoreCase("Action")).thenReturn(true);

        assertThatThrownBy(() -> genreService.createGenre(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Genre already exists");
    }

    @Test
    void updateGenre_ShouldReturnGenre_WhenValid() {
        GenreUpdateRequest request = new GenreUpdateRequest(" Sci-Fi ");
        when(genreRepository.findById(1)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByGenreNameIgnoreCaseAndIdNot("Sci-Fi", 1)).thenReturn(false);
        when(genreRepository.save(any())).thenReturn(genre);

        GenreResponse response = genreService.updateGenre(1, request);
        assertThat(response.getGenreName()).isEqualTo("Sci-Fi");
    }

    @Test
    void updateGenre_ShouldThrowConflict_WhenDuplicate() {
        GenreUpdateRequest request = new GenreUpdateRequest("Sci-Fi");
        when(genreRepository.findById(1)).thenReturn(Optional.of(genre));
        when(genreRepository.existsByGenreNameIgnoreCaseAndIdNot("Sci-Fi", 1)).thenReturn(true);

        assertThatThrownBy(() -> genreService.updateGenre(1, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Genre already exists");
    }

    @Test
    void updateGenre_ShouldThrowNotFound_WhenNotExists() {
        GenreUpdateRequest request = new GenreUpdateRequest("Sci-Fi");
        when(genreRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.updateGenre(1, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Genre not found");
    }
}
