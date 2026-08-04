package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.CreateMovieMediaRequest;
import com.lorafilm.movie.movie.dto.MovieMediaResponse;
import com.lorafilm.movie.movie.dto.UpdateMovieMediaRequest;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.service.MovieMediaServiceImpl;
import com.lorafilm.movie.movie.service.MovieOperationalGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MovieMediaServiceTest {

    @Mock
    private MovieMediaRepository movieMediaRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MovieOperationalGuard operationalGuard;

    @InjectMocks
    private MovieMediaServiceImpl movieMediaService;

    private Movie sampleMovie;
    private MovieMedia sampleMedia;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sampleMovie = new Movie();
        sampleMovie.setId(1L);
        sampleMovie.setPublicId("movie-uuid");
        sampleMovie.setTitle("Test Movie");
        sampleMovie.setSlug("test-movie-slug");
        sampleMovie.setStatus(MovieStatus.UPCOMING);

        sampleMedia = new MovieMedia();
        sampleMedia.setId(10L);
        sampleMedia.setPublicId("media-uuid");
        sampleMedia.setMovie(sampleMovie);
        sampleMedia.setMediaType(MovieMediaType.POSTER);
        sampleMedia.setUrl("http://example.com/poster.jpg");
        sampleMedia.setTitle("Poster 1");
        sampleMedia.setDisplayOrder(1);
        sampleMedia.setIsPrimary(true);
        sampleMedia.setStatus(ActiveStatus.ACTIVE);
    }

    @Test
    void createMedia_Success() {
        CreateMovieMediaRequest request = new CreateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieMediaRepository.save(any(MovieMedia.class))).thenAnswer(invocation -> {
            MovieMedia m = invocation.getArgument(0);
            m.setId(10L);
            return m;
        });

        MovieMediaResponse response = movieMediaService.createMedia("movie-uuid", request);

        assertNotNull(response);
        assertEquals("http://example.com/poster.jpg", response.getUrl());
        assertEquals(MovieMediaType.POSTER, response.getMediaType());
        assertTrue(response.getIsPrimary());
        assertEquals(ActiveStatus.ACTIVE, response.getStatus());

        verify(movieRepository, times(1)).findByPublicIdAndDeletedAtIsNull("movie-uuid");
        verify(movieMediaRepository, times(1)).resetPrimaryMedia(1L, MovieMediaType.POSTER);
        verify(movieMediaRepository, times(1)).save(any(MovieMedia.class));
    }

    @Test
    void createMedia_MovieNotFound() {
        CreateMovieMediaRequest request = new CreateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("unknown-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieMediaService.createMedia("unknown-uuid", request)
        );

        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
        verify(movieMediaRepository, never()).save(any());
    }

    @Test
    void createMedia_PrimaryInvalid() {
        CreateMovieMediaRequest request = new CreateMovieMediaRequest(
                MovieMediaType.TRAILER,
                "http://example.com/trailer.mp4",
                "Trailer 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieMediaService.createMedia("movie-uuid", request)
        );

        assertEquals(ErrorCode.MOVIE_PRIMARY_MEDIA_INVALID, exception.getErrorCode());
        verify(movieMediaRepository, never()).save(any());
    }

    @Test
    void updateMedia_Success() {
        UpdateMovieMediaRequest request = new UpdateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster-updated.jpg",
                "Poster 1 Updated",
                2,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaRepository.findByPublicIdAndDeletedAtIsNull("media-uuid")).thenReturn(Optional.of(sampleMedia));
        when(movieMediaRepository.save(any(MovieMedia.class))).thenReturn(sampleMedia);

        MovieMediaResponse response = movieMediaService.updateMedia("media-uuid", request);

        assertNotNull(response);
        assertEquals("http://example.com/poster-updated.jpg", response.getUrl());
        assertEquals("Poster 1 Updated", response.getTitle());
        assertEquals(2, response.getDisplayOrder());
        assertTrue(response.getIsPrimary());

        verify(movieMediaRepository, times(1)).findByPublicIdAndDeletedAtIsNull("media-uuid");
        verify(movieMediaRepository, times(1)).resetPrimaryMedia(1L, MovieMediaType.POSTER);
        verify(movieMediaRepository, times(1)).save(sampleMedia);
    }

    @Test
    void updateMedia_MediaNotFound() {
        UpdateMovieMediaRequest request = new UpdateMovieMediaRequest(
                MovieMediaType.POSTER,
                "http://example.com/poster.jpg",
                "Poster 1",
                1,
                true,
                ActiveStatus.ACTIVE
        );

        when(movieMediaRepository.findByPublicIdAndDeletedAtIsNull("unknown-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieMediaService.updateMedia("unknown-uuid", request)
        );

        assertEquals(ErrorCode.MOVIE_MEDIA_NOT_FOUND, exception.getErrorCode());
        verify(movieMediaRepository, never()).save(any());
    }

    @Test
    void deleteMedia_Success() {
        when(movieMediaRepository.findByPublicIdAndDeletedAtIsNull("media-uuid")).thenReturn(Optional.of(sampleMedia));
        when(currentUserProvider.getCurrentUserId()).thenReturn(42L);

        movieMediaService.deleteMedia("media-uuid");

        assertNotNull(sampleMedia.getDeletedAt());
        assertEquals(42L, sampleMedia.getDeletedBy());

        verify(movieMediaRepository, times(1)).save(sampleMedia);
    }

    @Test
    void getMovieMedia_Success() {
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieMediaRepository.findByMovieIdAndDeletedAtIsNull(1L)).thenReturn(List.of(sampleMedia));

        List<MovieMediaResponse> responses = movieMediaService.getMovieMedia("movie-uuid");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("media-uuid", responses.get(0).getPublicId());
    }

    @Test
    void getCustomerMedia_Success() {
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-uuid")).thenReturn(Optional.of(sampleMovie));
        when(movieMediaRepository.findByMovieIdAndStatusAndDeletedAtIsNull(1L, ActiveStatus.ACTIVE))
                .thenReturn(List.of(sampleMedia));

        List<MovieMediaResponse> responses = movieMediaService.getCustomerMedia("movie-uuid");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("media-uuid", responses.get(0).getPublicId());
    }

    @Test
    void getMedia_Success() {
        when(movieMediaRepository.findByPublicIdAndDeletedAtIsNull("media-uuid")).thenReturn(Optional.of(sampleMedia));

        MovieMediaResponse response = movieMediaService.getMedia("media-uuid");

        assertNotNull(response);
        assertEquals("media-uuid", response.getPublicId());
        assertEquals("Poster 1", response.getTitle());
    }

    @Test
    void getMedia_NotFound() {
        when(movieMediaRepository.findByPublicIdAndDeletedAtIsNull("unknown-uuid")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                movieMediaService.getMedia("unknown-uuid")
        );

        assertEquals(ErrorCode.MOVIE_MEDIA_NOT_FOUND, exception.getErrorCode());
    }
}
