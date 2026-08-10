package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.dto.MovieRevenueListResponse;
import com.project.analyticsservice.dto.TopMoviesResponse;
import com.project.analyticsservice.entity.MoviePerformanceDaily;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.MoviePerformanceDailyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieAnalyticsDomainServiceTest {
    @Mock
    private MoviePerformanceDailyRepository repository;
    private MovieAnalyticsDomainService service;

    @BeforeEach
    void setUp() {
        service = new MovieAnalyticsDomainService(repository, new MetricMathService());
    }

    @Test
    void lifetimeListAggregatesSnapshotsInsideService() {
        when(repository.findAll()).thenReturn(List.of(
                movie("101", 101L, "Avengers", LocalDate.of(2026, 7, 1), 10, "1000.00"),
                movie("101", 101L, "Avengers", LocalDate.of(2026, 7, 2), 5, "400.00")));

        MovieRevenueListResponse response = service.getMovieRevenueList(
                0, 10, null, null, null, null, "totalRevenue", "desc");

        assertEquals("LIFETIME", response.getMode());
        assertEquals(1, response.getContent().size());
        assertEquals(15, response.getContent().getFirst().getTotalTicketsSold());
        assertEquals(new BigDecimal("1400.00"), response.getContent().getFirst().getTotalRevenue());
    }

    @Test
    void rejectsRangesLongerThanLegacyContract() {
        assertThrows(BusinessException.class, () -> service.getMovieRevenueList(
                0, 10, null, null,
                "2026-01-01", "2026-05-01", "totalRevenue", "desc"));
    }

    @Test
    void topMoviesRanksAggregatedKpis() {
        when(repository.findAll()).thenReturn(List.of(
                movie("1", 1L, "Small", LocalDate.of(2026, 7, 1), 2, "100.00"),
                movie("2", 2L, "Big", LocalDate.of(2026, 7, 1), 3, "500.00")));

        TopMoviesResponse response = service.getTopMovies(
                "REVENUE", 1, "desc", null, null);

        assertEquals(1, response.getMovies().size());
        assertEquals("Big", response.getMovies().getFirst().getMovieTitle());
        assertEquals(1, response.getMovies().getFirst().getRank());
    }

    private MoviePerformanceDaily movie(
            String key, Long id, String title, LocalDate date, long tickets, String revenue) {
        MoviePerformanceDaily value = new MoviePerformanceDaily();
        value.setMovieKey(key);
        value.setMovieId(id);
        value.setMovieTitle(title);
        value.setStatDate(date);
        value.setTicketCount(tickets);
        value.setBookingCount(1L);
        value.setNetRevenue(new BigDecimal(revenue));
        value.setGrossRevenue(new BigDecimal(revenue));
        value.setDiscountAmount(BigDecimal.ZERO);
        value.setRefundAmount(BigDecimal.ZERO);
        value.setRefundRate(BigDecimal.ZERO);
        value.setOccupancyRate(BigDecimal.ZERO);
        value.setUpdatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        return value;
    }
}
