package com.lorafilm.movie.showtime.service.impl;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.dto.response.AdminShowtimeMapper;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminShowtimeQueryServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private MovieMediaRepository movieMediaRepository;

    private AdminShowtimeQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminShowtimeQueryServiceImpl(
                showtimeRepository,
                new AdminShowtimeMapper(),
                movieMediaRepository);
    }

    @Test
    void getAdminShowtimes_enrichesMovieWithActivePrimaryPoster() {
        Movie movie = new Movie();
        movie.setId(10L);
        movie.setPublicId("movie-10");
        movie.setSlug("poster-movie");
        movie.setTitle("Poster Movie");

        Showtime showtime = new Showtime();
        showtime.setPublicId("showtime-1");
        showtime.setMovie(movie);
        showtime.setServiceDate(LocalDate.of(2026, 8, 8));
        showtime.setStartTime(Instant.parse("2026-08-08T06:00:00Z"));
        showtime.setEndTime(Instant.parse("2026-08-08T08:00:00Z"));
        showtime.setStatus(ShowtimeStatus.DRAFT);

        MovieMedia poster = new MovieMedia();
        poster.setMovie(movie);
        poster.setUrl("https://cdn.example.com/poster.jpg");

        when(showtimeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(showtime)));
        when(movieMediaRepository.findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                List.of(10L), MovieMediaType.POSTER, ActiveStatus.ACTIVE))
                .thenReturn(List.of(poster));

        var response = service.getAdminShowtimes(null, null, null, null, null, null, 0, 25);

        assertEquals("https://cdn.example.com/poster.jpg",
                response.getData().getFirst().getMovie().getPosterUrl());
    }
}
