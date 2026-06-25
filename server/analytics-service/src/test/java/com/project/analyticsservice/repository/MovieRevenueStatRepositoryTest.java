package com.project.analyticsservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.project.analyticsservice.entity.MovieRevenueStat;

@DataJpaTest
class MovieRevenueStatRepositoryTest {

    @Autowired
    private MovieRevenueStatRepository movieRevenueStatRepository;

    @Test
    void testSaveAndFindByMovieId() {
        MovieRevenueStat stat = new MovieRevenueStat(
                null,
                101L,
                "Inception",
                500,
                new BigDecimal("4999.99"),
                null
        );

        MovieRevenueStat saved = movieRevenueStatRepository.save(stat);
        assertNotNull(saved.getId());
        assertNotNull(saved.getUpdatedAt());

        Optional<MovieRevenueStat> found = movieRevenueStatRepository.findByMovieId(101L);
        assertTrue(found.isPresent());
        assertEquals("Inception", found.get().getMovieTitle());
        assertEquals(500, found.get().getTotalTicketsSold());
        assertEquals(new BigDecimal("4999.99"), found.get().getTotalRevenue());

        assertTrue(movieRevenueStatRepository.existsByMovieId(101L));
        assertFalse(movieRevenueStatRepository.existsByMovieId(202L));
    }

    @Test
    void testUniqueMovieIdConstraint() {
        MovieRevenueStat stat1 = new MovieRevenueStat(null, 101L, "Inception", 500, new BigDecimal("4999.99"), null);
        movieRevenueStatRepository.saveAndFlush(stat1);

        MovieRevenueStat stat2 = new MovieRevenueStat(null, 101L, "Inception 2", 200, new BigDecimal("1999.99"), null);
        assertThrows(DataIntegrityViolationException.class, () -> {
            movieRevenueStatRepository.saveAndFlush(stat2);
        });
    }

    @Test
    void testPartialTitleSearchCaseInsensitive() {
        movieRevenueStatRepository.save(new MovieRevenueStat(null, 101L, "The Dark Knight", 100, new BigDecimal("1000.00"), null));
        movieRevenueStatRepository.save(new MovieRevenueStat(null, 102L, "The Dark Knight Rises", 200, new BigDecimal("2000.00"), null));
        movieRevenueStatRepository.save(new MovieRevenueStat(null, 103L, "Interstellar", 150, new BigDecimal("1500.00"), null));
        movieRevenueStatRepository.flush();

        Page<MovieRevenueStat> page = movieRevenueStatRepository.findByMovieTitleContainingIgnoreCase("dark", PageRequest.of(0, 10));
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(m -> m.getMovieTitle().contains("Dark")));

        Page<MovieRevenueStat> page2 = movieRevenueStatRepository.findByMovieTitleContainingIgnoreCase("rises", PageRequest.of(0, 10));
        assertEquals(1, page2.getTotalElements());
        assertEquals("The Dark Knight Rises", page2.getContent().get(0).getMovieTitle());
    }

    @Test
    void testTop10Queries() {
        for (int i = 1; i <= 12; i++) {
            movieRevenueStatRepository.save(new MovieRevenueStat(
                    null,
                    (long) i,
                    "Movie " + i,
                    i * 10,
                    new BigDecimal(String.valueOf(i * 100) + ".00"),
                    null
            ));
        }
        movieRevenueStatRepository.flush();

        // Top 10 by Revenue Desc
        List<MovieRevenueStat> topRevenue = movieRevenueStatRepository.findTop10ByOrderByTotalRevenueDesc();
        assertEquals(10, topRevenue.size());
        assertEquals("Movie 12", topRevenue.get(0).getMovieTitle());
        assertEquals(new BigDecimal("1200.00"), topRevenue.get(0).getTotalRevenue());
        assertEquals("Movie 3", topRevenue.get(9).getMovieTitle());

        // Top 10 by Tickets Desc
        List<MovieRevenueStat> topTickets = movieRevenueStatRepository.findTop10ByOrderByTotalTicketsSoldDesc();
        assertEquals(10, topTickets.size());
        assertEquals("Movie 12", topTickets.get(0).getMovieTitle());
        assertEquals(120, topTickets.get(0).getTotalTicketsSold());
        assertEquals(30, topTickets.get(9).getTotalTicketsSold());
    }

    @Test
    void testBigDecimalPrecision() {
        BigDecimal largeRevenue = new BigDecimal("999999999999.99");
        MovieRevenueStat stat = new MovieRevenueStat(
                null,
                999L,
                "Expensive Movie Production",
                1000000,
                largeRevenue,
                null
        );

        MovieRevenueStat saved = movieRevenueStatRepository.saveAndFlush(stat);
        MovieRevenueStat found = movieRevenueStatRepository.findById(saved.getId()).orElseThrow();
        assertEquals(largeRevenue, found.getTotalRevenue());
    }
}
