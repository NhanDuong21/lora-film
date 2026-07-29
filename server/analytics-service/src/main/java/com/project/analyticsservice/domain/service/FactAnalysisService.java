package com.project.analyticsservice.domain.service;

import com.project.analyticsservice.entity.FactBookingCancellation;
import com.project.analyticsservice.entity.FactBookingMetric;
import com.project.analyticsservice.entity.FactPaymentRefund;
import com.project.analyticsservice.repository.FactBookingCancellationRepository;
import com.project.analyticsservice.repository.FactBookingMetricRepository;
import com.project.analyticsservice.repository.FactPaymentRefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FactAnalysisService {
    private final FactBookingMetricRepository bookingRepository;
    private final FactBookingCancellationRepository cancellationRepository;
    private final FactPaymentRefundRepository refundRepository;
    private final MetricMathService math;

    public FactAnalysisService(
            FactBookingMetricRepository bookingRepository,
            FactBookingCancellationRepository cancellationRepository,
            FactPaymentRefundRepository refundRepository,
            MetricMathService math) {
        this.bookingRepository = bookingRepository;
        this.cancellationRepository = cancellationRepository;
        this.refundRepository = refundRepository;
        this.math = math;
    }

    public FactBundle load(LocalDate date) {
        List<FactBookingMetric> current = bookingRepository.findAllByBusinessDate(date);
        List<FactBookingMetric> historical = bookingRepository.findAllByBusinessDateLessThanEqual(date);
        Map<String, FactBookingMetric> byBooking = historical.stream()
                .sorted(Comparator.comparing(FactBookingMetric::getOccurredAt).reversed())
                .collect(Collectors.toMap(
                        FactBookingMetric::getBookingPublicId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        return new FactBundle(
                current,
                cancellationRepository.findAllByBusinessDate(date),
                refundRepository.findAllByRefundDate(date),
                historical,
                refundRepository.findAllByRefundDateLessThanEqual(date),
                byBooking);
    }

    public BigDecimal occupancyRate(List<FactBookingMetric> facts) {
        Map<String, Integer> capacityByShowtime = new HashMap<>();
        for (FactBookingMetric fact : facts) {
            if (StringUtils.hasText(fact.getShowtimePublicId())
                    && fact.getAvailableSeats() != null
                    && fact.getAvailableSeats() > 0) {
                capacityByShowtime.merge(
                        fact.getShowtimePublicId(), fact.getAvailableSeats(), Math::max);
            }
        }
        long soldSeats = facts.stream()
                .filter(fact -> StringUtils.hasText(fact.getShowtimePublicId()))
                .filter(fact -> capacityByShowtime.containsKey(fact.getShowtimePublicId()))
                .mapToLong(fact -> Math.max(0, fact.getTicketCount()))
                .sum();
        long availableSeats = capacityByShowtime.values().stream().mapToLong(Integer::longValue).sum();
        return math.ratio(soldSeats, availableSeats).min(BigDecimal.ONE);
    }

    public BigDecimal completeness(List<FactBookingMetric> facts) {
        if (facts.isEmpty()) {
            return BigDecimal.ONE.setScale(6);
        }
        long present = facts.stream().mapToLong(fact ->
                bool(present(fact.getMovieKey()))
                        + bool(present(fact.getCinemaPublicId()))
                        + bool(present(fact.getUserPublicId()))
                        + bool(present(fact.getShowtimePublicId())
                                && fact.getAvailableSeats() != null)
                        + bool(present(fact.getMembershipTier()))
                        + bool(present(fact.getPaymentMethod()))).sum();
        return math.ratio(present, facts.size() * 6L);
    }

    private boolean present(String value) {
        return StringUtils.hasText(value)
                && !"UNKNOWN".equalsIgnoreCase(value)
                && !"N/A".equalsIgnoreCase(value);
    }

    private int bool(boolean value) {
        return value ? 1 : 0;
    }

    public record FactBundle(
            List<FactBookingMetric> bookings,
            List<FactBookingCancellation> cancellations,
            List<FactPaymentRefund> refunds,
            List<FactBookingMetric> historicalBookings,
            List<FactPaymentRefund> historicalRefunds,
            Map<String, FactBookingMetric> bookingByKey) {
    }
}
