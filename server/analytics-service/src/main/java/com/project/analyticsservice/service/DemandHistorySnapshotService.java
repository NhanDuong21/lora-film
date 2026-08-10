package com.project.analyticsservice.service;

import com.project.analyticsservice.dto.DemandHistorySnapshotRequest;
import com.project.analyticsservice.dto.DemandHistorySnapshotResponse;
import com.project.analyticsservice.entity.FactBookingCancellation;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.entity.FactPaymentRefund;
import com.project.analyticsservice.repository.FactBookingCancellationRepository;
import com.project.analyticsservice.repository.FactBookingMetricRepository;
import com.project.analyticsservice.repository.FactPaymentRefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DemandHistorySnapshotService {

    public static final String SNAPSHOT_VERSION = "ANALYTICS_DEMAND_HISTORY_V1";
    private static final int RECENT_DAYS = 14;

    private final FactBookingMetricRepository bookingRepository;
    private final FactBookingCancellationRepository cancellationRepository;
    private final FactPaymentRefundRepository refundRepository;
    private final Clock clock;

    public DemandHistorySnapshotService(FactBookingMetricRepository bookingRepository,
                                        FactBookingCancellationRepository cancellationRepository,
                                        FactPaymentRefundRepository refundRepository,
                                        Clock clock) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.refundRepository = refundRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DemandHistorySnapshotResponse snapshot(DemandHistorySnapshotRequest request) {
        if (request.historyTo().isBefore(request.historyFrom())) {
            throw new IllegalArgumentException("historyTo must not be before historyFrom");
        }
        if (ChronoUnit.DAYS.between(request.historyFrom(), request.historyTo()) > 365) {
            throw new IllegalArgumentException("Demand history range cannot exceed 366 days");
        }
        ZoneId zoneId = ZoneId.of(request.cinemaTimezone());
        Set<String> requestedMovies = request.moviePublicIds().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        List<FactBookingMetric> cinemaFacts = bookingRepository
                .findAllByCinemaPublicIdAndBusinessDateBetween(
                        request.cinemaPublicId().trim(), request.historyFrom(), request.historyTo());
        List<FactBookingMetric> facts = cinemaFacts.stream()
                .filter(fact -> requestedMovies.isEmpty()
                        || requestedMovies.contains(fact.getMoviePublicId()))
                .toList();
        Set<String> bookingKeys = facts.stream().map(FactBookingMetric::getBookingPublicId)
                .filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.toSet());
        Set<String> refundedBookings = refundRepository
                .findAllByRefundDateBetween(request.historyFrom(), request.historyTo()).stream()
                .map(FactPaymentRefund::getBookingPublicId).filter(bookingKeys::contains)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> cancelledBookings = cancellationRepository
                .findAllByBusinessDateBetween(request.historyFrom(), request.historyTo()).stream()
                .map(FactBookingCancellation::getBookingKey).filter(bookingKeys::contains)
                .collect(java.util.stream.Collectors.toSet());

        DemandHistorySnapshotResponse.Aggregate cinemaPrior = aggregate(
                facts, refundedBookings, cancelledBookings,
                request.historyFrom(), request.historyTo());

        Map<String, List<FactBookingMetric>> byMovie = groupBy(
                facts, fact -> fact.getMoviePublicId() == null ? null : fact.getMoviePublicId());
        List<DemandHistorySnapshotResponse.MovieHistory> movies = byMovie.entrySet().stream()
                .map(entry -> new DemandHistorySnapshotResponse.MovieHistory(
                        entry.getKey(), aggregate(entry.getValue(), refundedBookings, cancelledBookings,
                        request.historyFrom(), request.historyTo())))
                .sorted(Comparator.comparing(DemandHistorySnapshotResponse.MovieHistory::moviePublicId))
                .toList();

        Map<SlotKey, List<FactBookingMetric>> bySlot = new HashMap<>();
        for (FactBookingMetric fact : facts) {
            if (fact.getShowtimeStartsAt() == null) continue;
            var localStart = fact.getShowtimeStartsAt().atZone(zoneId);
            boolean weekend = localStart.getDayOfWeek() == DayOfWeek.SATURDAY
                    || localStart.getDayOfWeek() == DayOfWeek.SUNDAY;
            bySlot.computeIfAbsent(new SlotKey(weekend, hourBucket(localStart.getHour())),
                    ignored -> new ArrayList<>()).add(fact);
        }
        List<DemandHistorySnapshotResponse.SlotHistory> slots = bySlot.entrySet().stream()
                .map(entry -> new DemandHistorySnapshotResponse.SlotHistory(
                        entry.getKey().weekend(), entry.getKey().hourBucket(),
                        aggregate(entry.getValue(), refundedBookings, cancelledBookings,
                                request.historyFrom(), request.historyTo())))
                .sorted(Comparator.comparing(DemandHistorySnapshotResponse.SlotHistory::weekend)
                        .thenComparing(DemandHistorySnapshotResponse.SlotHistory::hourBucket))
                .toList();

        Map<String, List<FactBookingMetric>> byFormat = groupBy(
                facts, fact -> fact.getFormat() == null ? null : fact.getFormat().trim().toUpperCase());
        List<DemandHistorySnapshotResponse.FormatHistory> formats = byFormat.entrySet().stream()
                .map(entry -> new DemandHistorySnapshotResponse.FormatHistory(
                        entry.getKey(), aggregate(entry.getValue(), refundedBookings, cancelledBookings,
                        request.historyFrom(), request.historyTo())))
                .sorted(Comparator.comparing(DemandHistorySnapshotResponse.FormatHistory::format))
                .toList();

        return new DemandHistorySnapshotResponse(
                SNAPSHOT_VERSION,
                clock.instant(),
                request.historyFrom(),
                request.historyTo(),
                facts.size(),
                facts.stream().filter(this::hasShowtimeContext).count(),
                cinemaPrior,
                movies,
                slots,
                formats);
    }

    private DemandHistorySnapshotResponse.Aggregate aggregate(
            List<FactBookingMetric> facts,
            Set<String> refundedBookings,
            Set<String> cancelledBookings,
            LocalDate from,
            LocalDate to) {
        Set<String> bookingKeys = new HashSet<>();
        Map<String, ShowtimeAggregate> showtimes = new LinkedHashMap<>();
        long ticketCount = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (FactBookingMetric fact : facts) {
            bookingKeys.add(fact.getBookingPublicId());
            ticketCount += fact.getTicketCount() == null ? 0 : fact.getTicketCount();
            revenue = revenue.add(fact.getNetRevenue() == null ? BigDecimal.ZERO : fact.getNetRevenue());
            if (hasShowtimeContext(fact)) {
                ShowtimeAggregate aggregate = showtimes.computeIfAbsent(
                        fact.getShowtimePublicId(), ignored -> new ShowtimeAggregate());
                aggregate.tickets += fact.getTicketCount() == null ? 0 : fact.getTicketCount();
                aggregate.capacity = max(aggregate.capacity, fact.getAvailableSeats());
            }
        }
        BigDecimal occupancySum = BigDecimal.ZERO;
        long occupancySamples = 0;
        for (ShowtimeAggregate showtime : showtimes.values()) {
            if (showtime.capacity != null && showtime.capacity > 0) {
                occupancySum = occupancySum.add(ratio(showtime.tickets, showtime.capacity).min(BigDecimal.ONE));
                occupancySamples++;
            }
        }
        BigDecimal averageOccupancy = occupancySamples == 0 ? BigDecimal.ZERO
                : occupancySum.divide(BigDecimal.valueOf(occupancySamples), 6, RoundingMode.HALF_UP);
        long inclusiveDays = Math.max(1L, ChronoUnit.DAYS.between(from, to) + 1L);
        LocalDate recentFrom = to.minusDays(RECENT_DAYS - 1L).isBefore(from)
                ? from : to.minusDays(RECENT_DAYS - 1L);
        long recentDays = Math.max(1L, ChronoUnit.DAYS.between(recentFrom, to) + 1L);
        long recentTickets = facts.stream().filter(fact -> !fact.getBusinessDate().isBefore(recentFrom))
                .mapToLong(fact -> fact.getTicketCount() == null ? 0 : fact.getTicketCount()).sum();
        long previousDays = Math.max(0L, ChronoUnit.DAYS.between(from, recentFrom));
        long previousTickets = facts.stream().filter(fact -> fact.getBusinessDate().isBefore(recentFrom))
                .mapToLong(fact -> fact.getTicketCount() == null ? 0 : fact.getTicketCount()).sum();
        long refundCount = bookingKeys.stream().filter(refundedBookings::contains).count();
        long cancellationCount = bookingKeys.stream().filter(cancelledBookings::contains).count();
        return new DemandHistorySnapshotResponse.Aggregate(
                bookingKeys.size(),
                showtimes.size(),
                ticketCount,
                averageOccupancy,
                ratio(ticketCount, inclusiveDays),
                ratio(recentTickets, recentDays),
                previousDays == 0 ? BigDecimal.ZERO : ratio(previousTickets, previousDays),
                ratio(refundCount, bookingKeys.size()),
                ratio(cancellationCount, bookingKeys.size()),
                ticketCount == 0 ? BigDecimal.ZERO
                        : revenue.divide(BigDecimal.valueOf(ticketCount), 2, RoundingMode.HALF_UP),
                occupancySamples > 0);
    }

    private <K> Map<K, List<FactBookingMetric>> groupBy(
            List<FactBookingMetric> facts,
            java.util.function.Function<FactBookingMetric, K> classifier) {
        Map<K, List<FactBookingMetric>> result = new LinkedHashMap<>();
        for (FactBookingMetric fact : facts) {
            K key = classifier.apply(fact);
            if (key != null) result.computeIfAbsent(key, ignored -> new ArrayList<>()).add(fact);
        }
        return result;
    }

    private boolean hasShowtimeContext(FactBookingMetric fact) {
        return fact.getShowtimePublicId() != null && fact.getShowtimeStartsAt() != null
                && fact.getAvailableSeats() != null && fact.getAvailableSeats() > 0;
    }

    private int hourBucket(int hour) {
        return (hour / 3) * 3;
    }

    private Integer max(Integer left, Integer right) {
        if (left == null) return right;
        if (right == null) return left;
        return Math.max(left, right);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO.setScale(6);
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 6, RoundingMode.HALF_UP);
    }

    private record SlotKey(boolean weekend, int hourBucket) {
    }

    private static final class ShowtimeAggregate {
        private long tickets;
        private Integer capacity;
    }
}
