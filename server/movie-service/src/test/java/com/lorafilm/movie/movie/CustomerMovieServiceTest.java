package com.lorafilm.movie.movie;

import com.lorafilm.movie.common.api.PageResponse;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Genre;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.MovieDto;
import com.lorafilm.movie.movie.dto.MovieMapper;
import com.lorafilm.movie.movie.repository.MovieGenreRepository;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.movie.service.CustomerMovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CustomerMovieServiceTest {

    @Mock
    private MovieRepository movieRepository;
    
    @Mock
    private MovieGenreRepository movieGenreRepository;

    @Mock
    private MovieMediaRepository movieMediaRepository;
    
    @Mock
    private MovieMapper movieMapper;

    @Mock
    private com.lorafilm.movie.movie.service.MovieService movieService;

    @InjectMocks
    private CustomerMovieService customerMovieService;

    private Movie activeMovie;
    private Movie draftMovie;

    @BeforeEach
    void setUp() {
        activeMovie = new Movie();
        activeMovie.setId(1L);
        activeMovie.setPublicId("movie-1");
        activeMovie.setStatus(MovieStatus.NOW_SHOWING);

        draftMovie = new Movie();
        draftMovie.setId(2L);
        draftMovie.setPublicId("movie-draft");
        draftMovie.setStatus(MovieStatus.DRAFT);
    }

    @Test
    void getMoviesByStatus_NowShowing_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Movie> moviePage = new PageImpl<>(List.of(activeMovie), pageable, 1);
        
        when(movieRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).thenReturn(moviePage);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());
        
        MovieDto dto = new MovieDto();
        dto.setPublicId("movie-1");
        when(movieMapper.toDto(eq(activeMovie), any(), any())).thenReturn(dto);

        PageResponse<MovieDto> response = customerMovieService.getMoviesByStatus("now-showing", null, pageable);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("movie-1", response.getContent().get(0).getPublicId());
    }

    @Test
    void getMoviesByStatus_All_ReturnsPubliclyVisibleMovies() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Movie> moviePage = new PageImpl<>(List.of(activeMovie), pageable, 1);

        when(movieRepository.findAll(
                any(org.springframework.data.jpa.domain.Specification.class),
                eq(pageable))).thenReturn(moviePage);
        when(movieGenreRepository.findByMovieId(1L)).thenReturn(Collections.emptyList());

        MovieDto dto = new MovieDto();
        dto.setPublicId("movie-1");
        when(movieMapper.toDto(eq(activeMovie), any(), any())).thenReturn(dto);

        PageResponse<MovieDto> response =
                customerMovieService.getMoviesByStatus("all", null, null, pageable);

        assertEquals(1, response.getContent().size());
        assertEquals("movie-1", response.getContent().get(0).getPublicId());
    }

    @Test
    void getMoviesByStatus_InvalidStatus_ThrowsException() {
        Pageable pageable = PageRequest.of(0, 10);
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> customerMovieService.getMoviesByStatus("invalid", null, pageable));
        
        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
    }

    @Test
    void getMovieDetail_Success() {
        when(movieRepository.findByIdentifierAndDeletedAtIsNull("movie-1")).thenReturn(Optional.of(activeMovie));
        
        com.lorafilm.movie.movie.dto.MovieDetailDto detailDto = new com.lorafilm.movie.movie.dto.MovieDetailDto();
        detailDto.setPublicId("movie-1");
        when(movieService.getMovieByIdentifier("movie-1")).thenReturn(detailDto);

        var response = customerMovieService.getMovieDetail("movie-1");
        assertNotNull(response);
        assertEquals("movie-1", response.getPublicId());
    }

    @Test
    void getMovieDetail_DraftMovie_ThrowsException() {
        when(movieRepository.findByIdentifierAndDeletedAtIsNull("movie-draft")).thenReturn(Optional.of(draftMovie));
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> customerMovieService.getMovieDetail("movie-draft"));
            
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }
    
    @Test
    void getMovieDetail_InactiveMovie_ThrowsException() {
        draftMovie.setStatus(MovieStatus.INACTIVE);
        when(movieRepository.findByIdentifierAndDeletedAtIsNull("movie-draft")).thenReturn(Optional.of(draftMovie));
        
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> customerMovieService.getMovieDetail("movie-draft"));
            
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }
}
