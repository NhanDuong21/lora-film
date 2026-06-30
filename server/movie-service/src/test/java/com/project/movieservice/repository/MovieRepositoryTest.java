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
import org.springframework.data.jpa.domain.Specification;

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

        Specification<Movie> spec = (root, query, cb) -> cb.equal(root.get("status"), MovieStatus.NOW_SHOWING);
        Page<Movie> page = movieRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void testFindAllPaginationWithMultipleGenres() {
        Genre genre1 = new Genre();
        genre1.setGenreName("Action");
        final Genre savedGenre1 = genreRepository.save(genre1);

        Genre genre2 = new Genre();
        genre2.setGenreName("Comedy");
        final Genre savedGenre2 = genreRepository.save(genre2);

        Movie movie = new Movie();
        movie.setTitle("Action Comedy");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2026, 6, 20));
        movie.setEndDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.NOW_SHOWING);
        movie.setAgeRating(AgeRating.T16);
        movie.getGenres().add(savedGenre1);
        movie.getGenres().add(savedGenre2);
        movieRepository.save(movie);

        // Fetch using specification with join on genres
        Specification<Movie> spec = (root, query, cb) -> {
            query.distinct(true);
            jakarta.persistence.criteria.Join<Movie, Genre> genreJoin = root.join("genres", jakarta.persistence.criteria.JoinType.INNER);
            return cb.equal(genreJoin.get("id"), savedGenre1.getId());
        };
        Page<Movie> page = movieRepository.findAll(spec, PageRequest.of(0, 10));

        // It should return 1 element, not duplicate due to multiple genres or join
        assertEquals(1, page.getTotalElements());
        assertEquals("Action Comedy", page.getContent().get(0).getTitle());
    }

    @Test
    void testFindInactiveMovies() {
        Movie movie = new Movie();
        movie.setTitle("Inactive Movie");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2026, 6, 20));
        movie.setEndDate(LocalDate.of(2026, 7, 20));
        movie.setStatus(MovieStatus.INACTIVE);
        movieRepository.save(movie);

        Specification<Movie> spec = (root, query, cb) -> cb.equal(root.get("status"), MovieStatus.INACTIVE);
        Page<Movie> page = movieRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}
