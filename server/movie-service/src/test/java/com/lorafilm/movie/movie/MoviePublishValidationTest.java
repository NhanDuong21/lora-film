package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Rule 1: Movie Publish Validation Business Rules")
class MoviePublishValidationTest {

    @Mock
    private MovieVersionRepository movieVersionRepository;

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should publish successfully when active version and primary poster exist")
    void validatePublishConditions_Success() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);

        assertDoesNotThrow(() -> movieService.validatePublishConditions(1L));

        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    @DisplayName("Should throw exception when active version is missing during publish validation")
    void validatePublishConditions_MissingVersion() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_ACTIVE_VERSION_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    @DisplayName("Should throw exception when primary poster is missing during publish validation")
    void validatePublishConditions_MissingPoster() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(true);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PRIMARY_POSTER_REQUIRED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }

    @Test
    @DisplayName("Should throw exception when both active version and primary poster are missing")
    void validatePublishConditions_MissingBoth() {
        when(movieVersionRepository.existsActiveVersion(1L)).thenReturn(false);
        when(movieMediaRepository.existsPrimaryPoster(1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieService.validatePublishConditions(1L)
        );

        assertEquals(ErrorCode.MOVIE_PUBLISH_VALIDATION_FAILED, exception.getErrorCode());
        verify(movieVersionRepository, times(1)).existsActiveVersion(1L);
        verify(movieMediaRepository, times(1)).existsPrimaryPoster(1L);
    }
}

