package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieServiceImpl;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.Genre;
import java.util.List;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoviePublishValidationTest {

    @Mock
    private MovieVersionRepository movieVersionRepository;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @Mock
    private MovieGenreRepository movieGenreRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void validatePublishConditions_Success() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        assertDoesNotThrow(() -> movieService.validatePublishConditions(1L));

        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
        verify(movieGenreRepository, times(1)).findByMovieId(1L);
    }

    @Test
    void validatePublishConditions_MissingVersion() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    void validatePublishConditions_MissingPoster() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    void validatePublishConditions_MissingBoth() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(List.of(new MovieGenre()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }
    
    @Test
    void validatePublishConditions_MissingGenre() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieGenreRepository, times(1)).findByMovieId(1L);
    }
}
