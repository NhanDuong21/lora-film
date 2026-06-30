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
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

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
    void testFindAllWithSpec_PaginationAndJoin() {
        Genre genre1 = genreRepository.save(new Genre(null, "Action"));
        Genre genre2 = genreRepository.save(new Genre(null, "Comedy"));

        Movie movie1 = new Movie();
        movie1.setTitle("Avengers 1");
        movie1.setDurationMinutes(180);
        movie1.setReleaseDate(LocalDate.now().minusDays(10));
        movie1.setEndDate(LocalDate.now().plusDays(20));
        movie1.setStatus(MovieStatus.NOW_SHOWING);
        movie1.getGenres().add(genre1);
        movie1.getGenres().add(genre2);
        movieRepository.save(movie1);

        Movie movie2 = new Movie();
        movie2.setTitle("Comedy Movie");
        movie2.setDurationMinutes(120);
        movie2.setReleaseDate(LocalDate.now().minusDays(10));
        movie2.setEndDate(LocalDate.now().plusDays(20));
        movie2.setStatus(MovieStatus.UPCOMING);
        movie2.getGenres().add(genre2);
        movieRepository.save(movie2);

        Specification<Movie> spec = (root, query, cb) -> {
            query.distinct(true);
            Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);
            return cb.equal(genreJoin.get("id"), genre2.getId());
        };

        Page<Movie> page = movieRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements()); // Pagination handles JOIN without duplicates
    }

    @Test
    void testFindAllWithSpec_InactiveVisibility() {
        Movie movie = new Movie();
        movie.setTitle("Old Movie");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.now().minusDays(100));
        movie.setEndDate(LocalDate.now().minusDays(80));
        movie.setStatus(MovieStatus.INACTIVE);
        movieRepository.save(movie);

        Specification<Movie> spec = (root, query, cb) -> cb.equal(root.get("status"), MovieStatus.INACTIVE);

        Page<Movie> page = movieRepository.findAll(spec, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        assertEquals("Old Movie", page.getContent().get(0).getTitle());
    }
}
