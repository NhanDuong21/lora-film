package com.lorafilm.movie.movie.service;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import com.lorafilm.movie.showtime.validation.ShowtimeOpeningPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieLaunchReadinessServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-04T03:00:00Z");

    @Mock private MovieRepository movieRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private MovieService movieService;
    @Mock private MovieApprovalPolicy approvalPolicy;
    @Mock private ShowtimeOpeningPolicy openingPolicy;

    private MovieLaunchReadinessService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"));
        service = new MovieLaunchReadinessService(
                movieRepository, showtimeRepository, movieService,
                approvalPolicy, openingPolicy, clock);
    }

    @Test
    void reportsBookableAndOpenableCountsForPublishedMovie() {
        Movie movie = movie(12L, MovieStatus.UPCOMING);
        Showtime draft = showtime(21L, "draft-1", ShowtimeStatus.DRAFT);
        Showtime open = showtime(22L, "open-1", ShowtimeStatus.OPEN_FOR_BOOKING);
        when(movieRepository.findByPublicIdAndDeletedAtIsNull("movie-1"))
                .thenReturn(Optional.of(movie));
        when(showtimeRepository
                .findByMovieIdAndStatusInAndStartTimeAfterAndDeletedAtIsNullOrderByStartTimeAsc(
                        12L, List.of(ShowtimeStatus.DRAFT, ShowtimeStatus.OPEN_FOR_BOOKING), NOW))
                .thenReturn(List.of(draft, open));
        when(openingPolicy.evaluate(draft, NOW))
                .thenReturn(new ShowtimeOpeningPolicy.Evaluation(true, List.of()));

        var result = service.get("movie-1");

        assertThat(result.contentReady()).isTrue();
        assertThat(result.publishable()).isTrue();
        assertThat(result.bookable()).isTrue();
        assertThat(result.futureShowtimeCount()).isEqualTo(2);
        assertThat(result.draftShowtimeCount()).isEqualTo(1);
        assertThat(result.openShowtimeCount()).isEqualTo(1);
        assertThat(result.openableDraftShowtimeCount()).isEqualTo(1);
        assertThat(result.blockers()).isEmpty();
    }

    private Movie movie(Long id, MovieStatus status) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setPublicId("movie-1");
        movie.setStatus(status);
        return movie;
    }

    private Showtime showtime(Long id, String publicId, ShowtimeStatus status) {
        Showtime showtime = new Showtime();
        showtime.setId(id);
        showtime.setPublicId(publicId);
        showtime.setStatus(status);
        showtime.setStartTime(NOW.plusSeconds(id));
        return showtime;
    }
}
