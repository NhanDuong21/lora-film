package com.project.analyticsservice.service;

import com.project.analyticsservice.dto.DemandHistorySnapshotRequest;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.repository.FactBookingCancellationRepository;
import com.project.analyticsservice.repository.FactBookingMetricRepository;
import com.project.analyticsservice.repository.FactPaymentRefundRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandHistorySnapshotServiceTest {

    @Mock FactBookingMetricRepository bookingRepository;
    @Mock FactBookingCancellationRepository cancellationRepository;
    @Mock FactPaymentRefundRepository refundRepository;

    @Test
    void aggregatesOnlyRequestedMoviesWithOccupancyAndStableVersion() {
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 30);
        FactBookingMetric requested = fact("booking-1", "movie-1", "show-1", 60, 100,
                new BigDecimal("6000000"), Instant.parse("2026-07-05T12:00:00Z"), from.plusDays(4));
        FactBookingMetric ignored = fact("booking-2", "movie-2", "show-2", 80, 100,
                new BigDecimal("8000000"), Instant.parse("2026-07-06T12:00:00Z"), from.plusDays(5));
        when(bookingRepository.findAllByCinemaPublicIdAndBusinessDateBetween("cinema-1", from, to))
                .thenReturn(List.of(requested, ignored));
        when(cancellationRepository.findAllByBusinessDateBetween(from, to)).thenReturn(List.of());
        when(refundRepository.findAllByRefundDateBetween(from, to)).thenReturn(List.of());
        var service = new DemandHistorySnapshotService(
                bookingRepository, cancellationRepository, refundRepository,
                Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneOffset.UTC));

        var response = service.snapshot(new DemandHistorySnapshotRequest(
                "cinema-1", from, to, "UTC", List.of("movie-1")));

        assertEquals(DemandHistorySnapshotService.SNAPSHOT_VERSION, response.snapshotVersion());
        assertEquals(1, response.sourceBookingFactCount());
        assertEquals(1, response.factsWithShowtimeContext());
        assertEquals(new BigDecimal("0.600000"), response.cinemaPrior().averageOccupancy());
        assertEquals(1, response.movies().size());
        assertEquals("movie-1", response.movies().getFirst().moviePublicId());
        assertTrue(response.cinemaPrior().hasShowtimeContext());
    }

    private FactBookingMetric fact(String bookingId, String movieId, String showtimeId,
                                   int tickets, int capacity, BigDecimal revenue,
                                   Instant start, LocalDate businessDate) {
        FactBookingMetric fact = new FactBookingMetric();
        fact.setBookingPublicId(bookingId);
        fact.setMoviePublicId(movieId);
        fact.setCinemaPublicId("cinema-1");
        fact.setShowtimePublicId(showtimeId);
        fact.setShowtimeStartsAt(start);
        fact.setFormat("TWO_D");
        fact.setTicketCount(tickets);
        fact.setAvailableSeats(capacity);
        fact.setNetRevenue(revenue);
        fact.setBusinessDate(businessDate);
        return fact;
    }
}
