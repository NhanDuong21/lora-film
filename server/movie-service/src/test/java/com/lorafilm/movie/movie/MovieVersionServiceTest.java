package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.movie.dto.CreateMovieVersionRequest;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieVersionRequest;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.movie.service.MovieVersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovieVersionServiceTest {

    @Mock
    private MovieVersionRepository movieVersionRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieVersionServiceImpl movieVersionService;

    private Movie sampleMovie;
    private MovieVersion sampleVersion;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleMovie = new Movie();
        sampleMovie.setId(1L);
        sampleMovie.setPublicId("movie-uuid");
        sampleMovie.setTitle("Test Movie");
        sampleMovie.setSlug("test-movie-slug");

        sampleVersion = new MovieVersion();
        sampleVersion.setId(10L);
        sampleVersion.setPublicId("version-uuid");
        sampleVersion.setMovie(sampleMovie);
        sampleVersion.setVersionName("2D Vietsub");
        sampleVersion.setFormat(MovieFormat.TWO_D);
        sampleVersion.setAudioLanguage("VI");
        sampleVersion.setSubtitleLanguage("EN");
        sampleVersion.setDubLanguage("NONE");
        sampleVersion.setStatus(ActiveStatus.ACTIVE);
    }

    @Test
    void createVersion_Success() {
        CreateMovieVersionRequest request = new CreateMovieVersionRequest(
                "2D Vietsub",
                MovieFormat.TWO_D,
                "vi",
                " en ",
                "none",
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieVersionRepository.existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguage(
                1L, MovieFormat.TWO_D, "VI", "EN", "NONE"
        )).thenReturn(false);
        when(movieVersionRepository.save(any(MovieVersion.class))).thenAnswer(invocation -> {
            MovieVersion v = invocation.getArgument(0);
            v.setId(20L);
            return v;
        });

        MovieVersionResponse response = movieVersionService.createVersion("movie-uuid", request);

        assertNotNull(response);
        assertEquals("2D Vietsub", response.getVersionName());
        assertEquals("VI", response.getAudioLanguage());
        assertEquals("EN", response.getSubtitleLanguage());
        assertEquals("NONE", response.getDubLanguage());
        assertEquals(ActiveStatus.ACTIVE, response.getStatus());

        verify(movieRepository, times(1)).findByPublicIdAndDeletedAtIsNull("movie-uuid");
        verify(movieVersionRepository, times(1)).save(any(MovieVersion.class));
    }

    @Test
    void createVersion_MovieNotFound() {
        CreateMovieVersionRequest request = new CreateMovieVersionRequest(
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("unknown-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieVersionService.createVersion("unknown-uuid", request)
        );

        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
        verify(movieVersionRepository, never()).save(any());
    }

    @Test
    void createVersion_DuplicateVersion() {
        CreateMovieVersionRequest request = new CreateMovieVersionRequest(
                "2D Vietsub",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieVersionRepository.existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguage(
                1L, MovieFormat.TWO_D, "VI", "EN", "NONE"
        )).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieVersionService.createVersion("movie-uuid", request)
        );

        assertEquals(ErrorCode.MOVIE_VERSION_DUPLICATED, exception.getErrorCode());
        verify(movieVersionRepository, never()).save(any());
    }

    @Test
    void updateVersion_Success() {
        UpdateMovieVersionRequest request = new UpdateMovieVersionRequest(
                "2D Vietsub Updated",
                MovieFormat.TWO_D,
                "VI",
                "EN",
                "NONE",
                ActiveStatus.INACTIVE
        );

        when(movieVersionRepository.findByPublicIdAndDeletedAtIsNull("version-uuid")).thenReturn(Optional.of(sampleVersion));
        when(movieVersionRepository.existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguageAndIdNot(
                1L, MovieFormat.TWO_D, "VI", "EN", "NONE", 10L
        )).thenReturn(false);
        when(movieVersionRepository.save(any(MovieVersion.class))).thenReturn(sampleVersion);

        MovieVersionResponse response = movieVersionService.updateVersion("version-uuid", request);

        assertNotNull(response);
        assertEquals("2D Vietsub Updated", response.getVersionName());
        assertEquals(ActiveStatus.INACTIVE, response.getStatus());

        verify(movieVersionRepository, times(1)).save(sampleVersion);
    }

    @Test
    void getActiveVersionsByMovie_Success() {
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieVersionRepository.findByMovieIdAndStatusAndDeletedAtIsNull(1L, ActiveStatus.ACTIVE))
                .thenReturn(Collections.singletonList(sampleVersion));

        List<MovieVersionResponse> responses = movieVersionService.getActiveVersionsByMovie("movie-uuid");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("version-uuid", responses.get(0).getPublicId());
    }

    @Test
    void getAllVersionsByMovie_Success() {
        MovieVersion inactiveVersion = new MovieVersion();
        inactiveVersion.setId(11L);
        inactiveVersion.setPublicId("version-uuid-inactive");
        inactiveVersion.setMovie(sampleMovie);
        inactiveVersion.setVersionName("3D Subtitle");
        inactiveVersion.setFormat(MovieFormat.THREE_D);
        inactiveVersion.setAudioLanguage("EN");
        inactiveVersion.setSubtitleLanguage("VI");
        inactiveVersion.setDubLanguage("NONE");
        inactiveVersion.setStatus(ActiveStatus.INACTIVE);

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieVersionRepository.findByMovieIdAndDeletedAtIsNull(1L))
                .thenReturn(List.of(sampleVersion, inactiveVersion));

        List<MovieVersionResponse> responses = movieVersionService.getAllVersionsByMovie("movie-uuid");

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("version-uuid", responses.get(0).getPublicId());
        assertEquals(ActiveStatus.ACTIVE, responses.get(0).getStatus());
        assertEquals("version-uuid-inactive", responses.get(1).getPublicId());
        assertEquals(ActiveStatus.INACTIVE, responses.get(1).getStatus());

        verify(movieRepository, times(1)).findByPublicIdAndDeletedAtIsNull("movie-uuid");
        verify(movieVersionRepository, times(1)).findByMovieIdAndDeletedAtIsNull(1L);
    }

    @Test
    void getAllVersionsByMovie_MovieNotFound() {
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("unknown-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieVersionService.getAllVersionsByMovie("unknown-uuid")
        );

        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
        verify(movieVersionRepository, never()).findByMovieIdAndDeletedAtIsNull(anyLong());
    }
}
