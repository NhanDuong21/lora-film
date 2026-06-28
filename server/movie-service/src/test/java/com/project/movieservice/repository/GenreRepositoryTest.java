package com.project.movieservice.repository;

import com.project.movieservice.entity.Genre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GenreRepositoryTest {

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void shouldSaveAndFindGenre() {
        Genre genre = new Genre(null, "Action");
        Genre saved = genreRepository.save(genre);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getGenreName()).isEqualTo("Action");

        Genre found = genreRepository.findById(saved.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getGenreName()).isEqualTo("Action");
    }

    @Test
    void shouldFindAllOrderedByName() {
        genreRepository.save(new Genre(null, "Zebra"));
        genreRepository.save(new Genre(null, "Action"));

        List<Genre> genres = genreRepository.findAllByOrderByGenreNameAsc();
        assertThat(genres.get(0).getGenreName()).isEqualTo("Action");
        assertThat(genres.get(1).getGenreName()).isEqualTo("Zebra");
    }

    @Test
    void shouldCheckExistsIgnoreCase() {
        genreRepository.save(new Genre(null, "Sci-Fi"));

        assertThat(genreRepository.existsByGenreNameIgnoreCase("sci-fi")).isTrue();
        assertThat(genreRepository.existsByGenreNameIgnoreCase("SCI-FI")).isTrue();
        assertThat(genreRepository.existsByGenreNameIgnoreCase("Drama")).isFalse();
    }

    @Test
    void shouldCheckExistsIgnoreCaseExcludeId() {
        Genre genre1 = genreRepository.save(new Genre(null, "Action"));
        genreRepository.save(new Genre(null, "Comedy"));

        assertThat(genreRepository.existsByGenreNameIgnoreCaseAndIdNot("action", genre1.getId())).isFalse();
        assertThat(genreRepository.existsByGenreNameIgnoreCaseAndIdNot("comedy", genre1.getId())).isTrue();
    }
}
