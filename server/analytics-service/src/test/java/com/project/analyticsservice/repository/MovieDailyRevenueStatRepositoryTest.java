package com.project.analyticsservice.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.analyticsservice.dto.MovieDateRangeAggregateProjection;
import com.project.analyticsservice.entity.MovieDailyRevenueStat;

@DataJpaTest
class MovieDailyRevenueStatRepositoryTest {

    @Autowired
    private MovieDailyRevenueStatRepository movieDailyRevenueStatRepository;

    @Test
    void testSaveAndFindMethods() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        MovieDailyRevenueStat stat = new MovieDailyRevenueStat(
                null,
                101L,
                "Inception",
                date,
                50,
                new BigDecimal("500.00"),
                null
        );

        MovieDailyRevenueStat saved = movieDailyRevenueStatRepository.save(stat);
        assertNotNull(saved.getId());
        assertNotNull(saved.getUpdatedAt());

        Optional<MovieDailyRevenueStat> found = movieDailyRevenueStatRepository.findByMovieIdAndStatDate(101L, date);
        assertTrue(found.isPresent());
        assertEquals("Inception", found.get().getMovieTitle());
        assertEquals(50, found.get().getTicketsSold());
        assertEquals(new BigDecimal("500.00"), found.get().getRevenue());
    }

    @Test
    void testUniqueMovieIdAndStatDateConstraint() {
        LocalDate date = LocalDate.of(2026, 6, 24);
        MovieDailyRevenueStat stat1 = new MovieDailyRevenueStat(
                null,
                101L,
                "Inception",
                date,
                50,
                new BigDecimal("500.00"),
                null
        );
        movieDailyRevenueStatRepository.saveAndFlush(stat1);

        MovieDailyRevenueStat stat2 = new MovieDailyRevenueStat(
                null,
                101L,
                "Inception",
                date,
                30,
                new BigDecimal("300.00"),
                null
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            movieDailyRevenueStatRepository.saveAndFlush(stat2);
        });
    }

    @Test
    void testFindAllByMovieIdAndStatDateBetweenOrderByStatDateAsc() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate mid = LocalDate.of(2026, 6, 22);
        LocalDate end = LocalDate.of(2026, 6, 24);

        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", start, 10, new BigDecimal("100.00"), null));
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", mid, 20, new BigDecimal("200.00"), null));
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", end, 30, new BigDecimal("300.00"), null));
        movieDailyRevenueStatRepository.flush();

        List<MovieDailyRevenueStat> list = movieDailyRevenueStatRepository
                .findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(101L, start, end);
        assertEquals(3, list.size());
        assertEquals(start, list.get(0).getStatDate());
        assertEquals(mid, list.get(1).getStatDate());
        assertEquals(end, list.get(2).getStatDate());
    }

    @Test
    void testFindAllByStatDateBetween() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate end = LocalDate.of(2026, 6, 22);

        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", start, 10, new BigDecimal("100.00"), null));
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 102L, "Avatar", end, 20, new BigDecimal("200.00"), null));
        movieDailyRevenueStatRepository.flush();

        List<MovieDailyRevenueStat> list = movieDailyRevenueStatRepository.findAllByStatDateBetween(start, end);
        assertEquals(2, list.size());
    }

    @Test
    void testAggregateMovieRevenueForDateRange() {
        LocalDate start = LocalDate.of(2026, 6, 20);
        LocalDate mid = LocalDate.of(2026, 6, 21);
        LocalDate end = LocalDate.of(2026, 6, 22);

        // Movie 101 records inside range
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", start, 10, new BigDecimal("100.00"), null));
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 101L, "Inception", mid, 15, new BigDecimal("150.00"), null));

        // Movie 102 records inside range
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 102L, "Avatar", mid, 20, new BigDecimal("250.00"), null));
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 102L, "Avatar", end, 30, new BigDecimal("350.00"), null));

        // Movie 103 record outside range
        movieDailyRevenueStatRepository.save(new MovieDailyRevenueStat(null, 103L, "Titanic", start.minusDays(1), 50, new BigDecimal("500.00"), null));

        movieDailyRevenueStatRepository.flush();

        List<MovieDateRangeAggregateProjection> results = movieDailyRevenueStatRepository
                .aggregateMovieRevenueForDateRange(start, end);

        assertEquals(2, results.size());

        MovieDateRangeAggregateProjection p101 = results.stream()
                .filter(p -> p.getMovieId().equals(101L))
                .findFirst().orElseThrow();
        assertEquals("Inception", p101.getMovieTitle());
        assertEquals(25L, p101.getTotalTicketsSold());
        assertEquals(new BigDecimal("250.00"), p101.getTotalRevenue());
        assertNotNull(p101.getLastUpdatedAt());

        MovieDateRangeAggregateProjection p102 = results.stream()
                .filter(p -> p.getMovieId().equals(102L))
                .findFirst().orElseThrow();
        assertEquals("Avatar", p102.getMovieTitle());
        assertEquals(50L, p102.getTotalTicketsSold());
        assertEquals(new BigDecimal("600.00"), p102.getTotalRevenue());
        assertNotNull(p102.getLastUpdatedAt());
    }
}
