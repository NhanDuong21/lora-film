package com.project.analyticsservice.service.impl;

import com.project.analyticsservice.dto.*;
import com.project.analyticsservice.entity.MovieDailyRevenueStat;
import com.project.analyticsservice.entity.MovieRevenueStat;
import com.project.analyticsservice.exception.BusinessException;
import com.project.analyticsservice.repository.MovieDailyRevenueStatRepository;
import com.project.analyticsservice.repository.MovieRevenueStatRepository;
import com.project.analyticsservice.dto.MovieDateRangeAggregateProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieAnalyticsServiceImplTest {

    @Mock
    private MovieRevenueStatRepository movieRevenueStatRepository;

    @Mock
    private MovieDailyRevenueStatRepository movieDailyRevenueStatRepository;

    @InjectMocks
    private MovieAnalyticsServiceImpl movieAnalyticsService;

    private MovieRevenueStat lifetimeMovie;
    private MovieDailyRevenueStat dailyMovie;

    @BeforeEach
    void setUp() {
        lifetimeMovie = new MovieRevenueStat(1L, 101L, "Avengers", 850, new BigDecimal("98500000.00"), LocalDateTime.now());
        dailyMovie = new MovieDailyRevenueStat(1L, 101L, "Avengers", LocalDate.of(2026, 6, 21), 24, new BigDecimal("2780000.00"), LocalDateTime.now());
    }

    @Test
    void getMovieRevenueList_Lifetime_Success() {
        Page<MovieRevenueStat> page = new PageImpl<>(List.of(lifetimeMovie), PageRequest.of(0, 10), 1);
        when(movieRevenueStatRepository.searchLifetime(any(), any(), any())).thenReturn(page);

        MovieRevenueListResponse response = movieAnalyticsService.getMovieRevenueList(
                0, 10, null, null, null, null, "totalRevenue", "desc");

        assertNotNull(response);
        assertEquals("LIFETIME", response.getMode());
        assertNull(response.getPeriod());
        assertEquals(1, response.getContent().size());
        assertEquals("Avengers", response.getContent().get(0).getMovieTitle());
        assertEquals(new BigDecimal("98500000.00"), response.getContent().get(0).getTotalRevenue());
    }

    @Test
    void getMovieRevenueList_DateRange_Success() {
        MovieDateRangeAggregateProjection proj = mock(MovieDateRangeAggregateProjection.class);
        when(proj.getMovieId()).thenReturn(101L);
        when(proj.getMovieTitle()).thenReturn("Avengers");
        when(proj.getTotalTicketsSold()).thenReturn(420L);
        when(proj.getTotalRevenue()).thenReturn(new BigDecimal("48600000.00"));
        when(proj.getLastUpdatedAt()).thenReturn(LocalDateTime.now());

        Page<MovieDateRangeAggregateProjection> page = new PageImpl<>(List.of(proj), PageRequest.of(0, 10), 1);
        when(movieDailyRevenueStatRepository.aggregateMovieRevenueForDateRangeWithFilters(
                any(), any(), any(), any(), any())).thenReturn(page);

        MovieRevenueListResponse response = movieAnalyticsService.getMovieRevenueList(
                0, 10, 101L, "Avengers", "2026-06-01", "2026-06-21", "totalRevenue", "desc");

        assertNotNull(response);
        assertEquals("DATE_RANGE", response.getMode());
        assertNotNull(response.getPeriod());
        assertEquals("2026-06-01", response.getPeriod().getStartDate());
        assertEquals(1, response.getContent().size());
        assertEquals("Avengers", response.getContent().get(0).getMovieTitle());
        assertEquals(420, response.getContent().get(0).getTotalTicketsSold());
    }

    @Test
    void getMovieRevenueList_PartialDateRange_ThrowsException() {
        assertThrows(BusinessException.class, () -> movieAnalyticsService.getMovieRevenueList(
                0, 10, null, null, "2026-06-01", null, "totalRevenue", "desc"));
    }

    @Test
    void getMovieRevenueList_DateRangeTooLarge_ThrowsException() {
        assertThrows(BusinessException.class, () -> movieAnalyticsService.getMovieRevenueList(
                0, 10, null, null, "2026-06-01", "2026-10-01", "totalRevenue", "desc"));
    }

    @Test
    void getMovieRevenueList_InvalidSort_ThrowsException() {
        assertThrows(BusinessException.class, () -> movieAnalyticsService.getMovieRevenueList(
                0, 10, null, null, null, null, "invalidField", "desc"));
    }

    @Test
    void getMovieRevenueDetail_Lifetime_Success() {
        when(movieRevenueStatRepository.findByMovieId(101L)).thenReturn(Optional.of(lifetimeMovie));

        MovieRevenueDetailResponse response = movieAnalyticsService.getMovieRevenueDetail(101L, null, null);

        assertNotNull(response);
        assertEquals("LIFETIME", response.getMode());
        assertEquals(850, response.getTotalTicketsSold());
        assertEquals(new BigDecimal("98500000.00"), response.getTotalRevenue());
        // 98500000 / 850 = 115882.35
        assertEquals(new BigDecimal("115882.35"), response.getAverageRevenuePerTicket());
    }

    @Test
    void getMovieRevenueDetail_Lifetime_NotFound() {
        when(movieRevenueStatRepository.findByMovieId(101L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> movieAnalyticsService.getMovieRevenueDetail(101L, null, null));
    }

    @Test
    void getMovieRevenueDetail_ZeroTicketsSold_AverageIsZero() {
        lifetimeMovie.setTotalTicketsSold(0);
        when(movieRevenueStatRepository.findByMovieId(101L)).thenReturn(Optional.of(lifetimeMovie));

        MovieRevenueDetailResponse response = movieAnalyticsService.getMovieRevenueDetail(101L, null, null);

        assertEquals(new BigDecimal("0.00"), response.getAverageRevenuePerTicket());
    }

    @Test
    void getMovieRevenueDetail_NegativeRevenue_AverageIsNegative() {
        lifetimeMovie.setTotalRevenue(new BigDecimal("-100000.00"));
        lifetimeMovie.setTotalTicketsSold(2);
        when(movieRevenueStatRepository.findByMovieId(101L)).thenReturn(Optional.of(lifetimeMovie));

        MovieRevenueDetailResponse response = movieAnalyticsService.getMovieRevenueDetail(101L, null, null);

        assertEquals(new BigDecimal("-50000.00"), response.getAverageRevenuePerTicket());
    }

    @Test
    void getMovieRevenueTrend_IncludeEmptyDates_Success() {
        when(movieDailyRevenueStatRepository.findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(anyLong(), any(), any()))
                .thenReturn(List.of(dailyMovie));

        MovieRevenueTrendResponse response = movieAnalyticsService.getMovieRevenueTrend(
                101L, "2026-06-19", "2026-06-21", true);

        assertNotNull(response);
        assertEquals(3, response.getStatistics().size());
        assertEquals("2026-06-19", response.getStatistics().get(0).getStatDate());
        assertEquals(0, response.getStatistics().get(0).getTicketsSold());
        assertEquals(BigDecimal.ZERO, response.getStatistics().get(0).getRevenue());

        assertEquals("2026-06-21", response.getStatistics().get(2).getStatDate());
        assertEquals(24, response.getStatistics().get(2).getTicketsSold());
    }

    @Test
    void getMovieRevenueTrend_ExcludeEmptyDates_Success() {
        when(movieDailyRevenueStatRepository.findAllByMovieIdAndStatDateBetweenOrderByStatDateAsc(anyLong(), any(), any()))
                .thenReturn(List.of(dailyMovie));

        MovieRevenueTrendResponse response = movieAnalyticsService.getMovieRevenueTrend(
                101L, "2026-06-19", "2026-06-21", false);

        assertNotNull(response);
        assertEquals(1, response.getStatistics().size());
        assertEquals("2026-06-21", response.getStatistics().get(0).getStatDate());
    }

    @Test
    void getTopMovies_Lifetime_Success() {
        Page<MovieRevenueStat> page = new PageImpl<>(List.of(lifetimeMovie), PageRequest.of(0, 10), 1);
        when(movieRevenueStatRepository.findAll(any(Pageable.class))).thenReturn(page);

        TopMoviesResponse response = movieAnalyticsService.getTopMovies(
                "REVENUE", 10, "desc", null, null);

        assertNotNull(response);
        assertEquals("LIFETIME", response.getMode());
        assertEquals("REVENUE", response.getMetric());
        assertEquals(1, response.getMovies().size());
        assertEquals(1, response.getMovies().get(0).getRank());
        assertEquals("Avengers", response.getMovies().get(0).getMovieTitle());
    }
}
