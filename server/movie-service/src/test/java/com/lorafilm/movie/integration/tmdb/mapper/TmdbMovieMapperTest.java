package com.lorafilm.movie.integration.tmdb.mapper;

import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieDetailsDto;
import com.lorafilm.movie.integration.tmdb.dto.TmdbMovieWrapperDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TmdbMovieMapperTest {

    private final TmdbMovieMapper mapper = Mappers.getMapper(TmdbMovieMapper.class);

    @Test
    void oldMovieKeepsOriginalDateButHasNoLocalExhibitionDate() {
        TmdbMovieWrapperDto wrapper = movieWithReleaseDate("1999-10-15");

        var movie = mapper.toEntity(wrapper);

        assertEquals(LocalDate.of(1999, 10, 15), movie.getOriginalReleaseDate());
        assertNull(movie.getReleaseDate());
    }

    @Test
    void futureMovieUsesOriginalDateAsInitialExhibitionPlan() {
        LocalDate futureDate = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMonths(2);
        TmdbMovieWrapperDto wrapper = movieWithReleaseDate(futureDate.toString());

        var movie = mapper.toEntity(wrapper);

        assertEquals(futureDate, movie.getOriginalReleaseDate());
        assertEquals(futureDate, movie.getReleaseDate());
    }

    private TmdbMovieWrapperDto movieWithReleaseDate(String releaseDate) {
        TmdbMovieDetailsDto details = new TmdbMovieDetailsDto();
        details.setTitle("Phim kiểm thử");
        details.setOriginalTitle("Test Movie");
        details.setReleaseDate(releaseDate);
        details.setRuntimeMinutes(100);
        details.setAdult(false);

        TmdbMovieWrapperDto wrapper = new TmdbMovieWrapperDto();
        wrapper.setTmdbId(123L);
        wrapper.setMovie(details);
        return wrapper;
    }
}
