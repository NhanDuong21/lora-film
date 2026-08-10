package com.lorafilm.movie.showtime.service;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import com.lorafilm.movie.movie.repository.MovieMediaRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.seat.service.SeatService;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.dto.ShowtimeDto;
import com.lorafilm.movie.showtime.dto.ShowtimeMapper;
import com.lorafilm.movie.showtime.dto.ShowtimeMovieDto;
import com.lorafilm.movie.showtime.repository.ShowtimeBlockedSeatRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimePosterProjectionTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private ShowtimePriceRepository showtimePriceRepository;
    @Mock
    private ShowtimeBlockedSeatRepository showtimeBlockedSeatRepository;
    @Mock
    private SeatService seatService;
    @Mock
    private ShowtimeMapper showtimeMapper;
    @Mock
    private MovieMediaRepository movieMediaRepository;

    @Test
    @SuppressWarnings("unchecked")
    void getShowtimesAddsTheActivePrimaryPosterToEachMovie() {
        Movie movie = new Movie();
        movie.setId(8L);
        movie.setPublicId("movie-8");

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);

        ShowtimeMovieDto movieDto = new ShowtimeMovieDto();
        movieDto.setPublicId("movie-8");
        ShowtimeDto showtimeDto = new ShowtimeDto();
        showtimeDto.setMovie(movieDto);

        MovieMedia poster = new MovieMedia();
        poster.setMovie(movie);
        poster.setUrl("https://cdn.lorafilm.test/movie-8.jpg");

        when(showtimeRepository.findAll(
                any(Specification.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(showtime)));
        when(showtimeMapper.toDto(showtime)).thenReturn(showtimeDto);
        when(movieMediaRepository
                .findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(
                        eq(List.of(8L)),
                        eq(MovieMediaType.POSTER),
                        eq(ActiveStatus.ACTIVE)))
                .thenReturn(List.of(poster));

        ShowtimeQueryServiceImpl service = new ShowtimeQueryServiceImpl(
                showtimeRepository,
                showtimePriceRepository,
                showtimeBlockedSeatRepository,
                seatService,
                showtimeMapper,
                movieMediaRepository);

        var result = service.getShowtimes(
                null, null, null, null, null, null, null, 0, 10);

        assertEquals(
                "https://cdn.lorafilm.test/movie-8.jpg",
                result.getData().getFirst().getMovie().getPosterUrl());
    }
}
