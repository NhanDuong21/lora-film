package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieExhibitionPeriod;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.dto.CreateMovieExhibitionPeriodRequest;
import com.lorafilm.movie.movie.repository.MovieExhibitionPeriodRepository;
import com.lorafilm.movie.movie.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieExhibitionPeriodServiceTest {

    @Mock private MovieRepository movieRepository;
    @Mock private MovieExhibitionPeriodRepository periodRepository;
    @Mock private MovieOperationalGuard operationalGuard;

    private MovieExhibitionPeriodService service;
    private Movie movie;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-07T03:00:00Z"),
                ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new MovieExhibitionPeriodService(
                movieRepository, periodRepository, operationalGuard, clock);

        movie = new Movie();
        movie.setId(1L);
        movie.setPublicId("movie-1");
        movie.setStatus(MovieStatus.ENDED);
        movie.setOriginalReleaseDate(LocalDate.of(1999, 10, 15));
        movie.setReleaseDate(LocalDate.of(2020, 1, 1));
        movie.setEndDate(LocalDate.of(2020, 1, 31));
        org.mockito.Mockito.lenient().when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-1")).thenReturn(Optional.of(movie));
        org.mockito.Mockito.lenient().when(periodRepository.existsByMovieIdAndStartDateAndEndDateAndDeletedAtIsNull(
                1L, movie.getReleaseDate(), movie.getEndDate())).thenReturn(false);
        org.mockito.Mockito.lenient().when(periodRepository.save(any(MovieExhibitionPeriod.class))).thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(movieRepository.save(movie)).thenReturn(movie);
    }

    @Test
    void createsNewPeriodWithoutChangingOriginalReleaseDate() {
        CreateMovieExhibitionPeriodRequest request = new CreateMovieExhibitionPeriodRequest();
        request.setStartDate(LocalDate.of(2026, 8, 20));
        request.setEndDate(LocalDate.of(2026, 9, 5));
        request.setNote("Chiếu lại phim kinh điển");

        var response = service.createPeriod("movie-1", request);

        assertEquals(LocalDate.of(1999, 10, 15), movie.getOriginalReleaseDate());
        assertEquals(LocalDate.of(2026, 8, 20), movie.getReleaseDate());
        assertEquals(LocalDate.of(2026, 9, 5), movie.getEndDate());
        assertEquals("UPCOMING", response.periodState());
        verify(periodRepository, org.mockito.Mockito.times(2)).save(any(MovieExhibitionPeriod.class));
        verify(movieRepository).save(movie);
    }

    @Test
    void rejectsCurrentPeriodAsADuplicate() {
        movie.setReleaseDate(LocalDate.of(2026, 8, 20));
        movie.setEndDate(LocalDate.of(2026, 9, 5));
        CreateMovieExhibitionPeriodRequest request = new CreateMovieExhibitionPeriodRequest();
        request.setStartDate(movie.getReleaseDate());
        request.setEndDate(movie.getEndDate());

        assertThrows(BusinessException.class, () -> service.createPeriod("movie-1", request));

        verify(periodRepository, never()).save(any(MovieExhibitionPeriod.class));
        verify(movieRepository, never()).save(movie);
    }

    @Test
    void rejectsNewPeriodThatDoesNotStartAfterToday() {
        CreateMovieExhibitionPeriodRequest request = new CreateMovieExhibitionPeriodRequest();
        request.setStartDate(LocalDate.of(2026, 8, 7));

        assertThrows(BusinessException.class, () -> service.createPeriod("movie-1", request));

        verify(periodRepository, never()).save(any(MovieExhibitionPeriod.class));
        verify(movieRepository, never()).save(movie);
    }

    @Test
    void rejectsNewPeriodForMovieThatIsStillBeingShown() {
        movie.setStatus(MovieStatus.NOW_SHOWING);
        CreateMovieExhibitionPeriodRequest request = new CreateMovieExhibitionPeriodRequest();
        request.setStartDate(LocalDate.of(2026, 8, 20));

        assertThrows(BusinessException.class, () -> service.createPeriod("movie-1", request));

        verify(periodRepository, never()).save(any(MovieExhibitionPeriod.class));
        verify(movieRepository, never()).save(movie);
    }
}
