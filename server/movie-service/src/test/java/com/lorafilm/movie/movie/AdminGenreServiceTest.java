package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.dto.GenreRequest;
import com.lorafilm.movie.movie.dto.GenreResponse;
import com.lorafilm.movie.movie.repository.GenreRepository;
import com.lorafilm.movie.movie.service.AdminGenreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AdminGenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private com.lorafilm.movie.movie.dto.GenreMapper genreMapper;

    @InjectMocks
    private AdminGenreService adminGenreService;

    private Genre existingGenre;

    @BeforeEach
    void setUp() {
        existingGenre = new Genre();
        existingGenre.setId(1L);
        existingGenre.setPublicId("genre-id");
        existingGenre.setName("Action");
        existingGenre.setSlug("action");
        existingGenre.setStatus(ActiveStatus.ACTIVE);
    }

    @Test
    void createGenre_Success() {
        GenreRequest request = new GenreRequest();
        request.setName("Comedy");

        Genre savedGenre = new Genre();
        savedGenre.setId(2L);
        savedGenre.setPublicId("new-genre-id");
        savedGenre.setName("Comedy");
        savedGenre.setSlug("comedy");
        savedGenre.setStatus(ActiveStatus.ACTIVE);

        when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);

        GenreResponse mockResponse = new GenreResponse();
        mockResponse.setName("Comedy");
        mockResponse.setSlug("comedy");
        when(genreMapper.toResponse(any(Genre.class))).thenReturn(mockResponse);

        GenreResponse response = adminGenreService.createGenre(request);

        assertNotNull(response);
        assertEquals("Comedy", response.getName());
        assertEquals("comedy", response.getSlug());
    }

    @Test
    void updateGenre_Success() {
        GenreRequest request = new GenreRequest();
        request.setName("Action Update");
        request.setStatus(ActiveStatus.INACTIVE);

        when(genreRepository.findByPublicIdAndDeletedAtIsNull("genre-id")).thenReturn(Optional.of(existingGenre));
        
        Genre updatedGenre = new Genre();
        updatedGenre.setId(1L);
        updatedGenre.setPublicId("genre-id");
        updatedGenre.setName("Action Update");
        updatedGenre.setSlug("action-update");
        updatedGenre.setStatus(ActiveStatus.INACTIVE);

        when(genreRepository.save(any(Genre.class))).thenReturn(updatedGenre);

        GenreResponse mockResponse = new GenreResponse();
        mockResponse.setName("Action Update");
        mockResponse.setSlug("action-update");
        mockResponse.setStatus(ActiveStatus.INACTIVE);
        when(genreMapper.toResponse(any(Genre.class))).thenReturn(mockResponse);

        GenreResponse response = adminGenreService.updateGenre("genre-id", request);

        assertNotNull(response);
        assertEquals("Action Update", response.getName());
        assertEquals("action-update", response.getSlug());
        assertEquals(ActiveStatus.INACTIVE, response.getStatus());
    }

    @Test
    void updateGenre_NotFound_ThrowsException() {
        GenreRequest request = new GenreRequest();
        request.setName("Action Update");

        when(genreRepository.findByPublicIdAndDeletedAtIsNull("non-existent")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> adminGenreService.updateGenre("non-existent", request));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }
}
