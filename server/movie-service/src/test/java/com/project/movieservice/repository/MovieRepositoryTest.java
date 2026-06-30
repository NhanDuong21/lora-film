package com.project.movieservice.repository;

import com.project.movieservice.entity.Genre;
import com.project.movieservice.entity.Movie;
import com.project.movieservice.enumtype.AgeRating;
import com.project.movieservice.enumtype.MovieStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void testSaveAndFindById() {
        Genre genre = new Genre();
        genre.setGenreName("Action");
        genre = genreRepository.save(genre);

        Movie movie = new Movie();
        movie.setTitle("Avengers");
        movie.setDurationMinutes(180);
        movie.setReleaseDate(LocalDate.of(2026, 6, 20));
        movie.setEndDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setAgeRating(AgeRating.T16);
        movie.getGenres().add(genre);

        movie = movieRepository.save(movie);

        Optional<Movie> found = movieRepository.findById(movie.getId());
        assertTrue(found.isPresent());
        assertEquals("Avengers", found.get().getTitle());
        assertEquals(1, found.get().getGenres().size());
    }

    @Test
    void testFindAllWithSpec() {
        Movie movie = new Movie();
        movie.setTitle("Avengers");
        movie.setDurationMinutes(180);
        movie.setReleaseDate(LocalDate.of(2026, 6, 20));
        movie.setEndDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movieRepository.save(movie);

        Page<Movie> page = movieRepository.findAll((root, query, cb) -> cb.equal(root.get("status"), MovieStatus.NOW_SHOWING), PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}
