package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.DemandCandidateFacts;
import com.lorafilm.movie.autoschedule.model.DemandEstimate;
import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;
import com.lorafilm.movie.autoschedule.service.DemandEngine;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AutomaticDemandEngineV1 implements DemandEngine {

    public static final String MODEL_VERSION = "AUTO_DEMAND_HEURISTIC_V1";
    private static final BigDecimal COLD_START_OCCUPANCY = new BigDecimal("0.32");

    @Override
    public String modelVersion() {
        return MODEL_VERSION;
    }

    @Override
    public DemandEstimate estimate(DemandCandidateFacts candidate, DemandHistorySnapshot history) {
        var localStart = candidate.startTime().atZone(candidate.cinemaZone());
        boolean weekend = localStart.getDayOfWeek() == DayOfWeek.SATURDAY
                || localStart.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean primeTime = localStart.getHour() >= 18 && localStart.getHour() < 22;
        int bucket = (localStart.getHour() / 3) * 3;

        DemandHistorySnapshot.Aggregate cinema = safe(history.cinemaPrior());
        DemandHistorySnapshot.Aggregate movie = history.movies().stream()
                .filter(item -> candidate.moviePublicId().equals(item.moviePublicId()))
                .map(DemandHistorySnapshot.MovieHistory::aggregate).findFirst().orElse(null);
        DemandHistorySnapshot.Aggregate slot = history.slots().stream()
                .filter(item -> item.weekend() == weekend && item.hourBucket() == bucket)
                .map(DemandHistorySnapshot.SlotHistory::aggregate).findFirst().orElse(null);
        DemandHistorySnapshot.Aggregate format = history.formats().stream()
                .filter(item -> candidate.format() != null
                        && candidate.format().name().equalsIgnoreCase(item.format()))
                .map(DemandHistorySnapshot.FormatHistory::aggregate).findFirst().orElse(null);

        BigDecimal occupancy = cinema.hasShowtimeContext()
                ? clamp(cinema.averageOccupancy(), new BigDecimal("0.10"), new BigDecimal("0.90"))
                : COLD_START_OCCUPANCY;
        BigDecimal totalWeight = BigDecimal.ONE;
        if (slot != null && slot.showtimeCount() >= 3 && slot.hasShowtimeContext()) {
            occupancy = weighted(occupancy, totalWeight, slot.averageOccupancy(), new BigDecimal("1.25"));
            totalWeight = totalWeight.add(new BigDecimal("1.25"));
        }
        if (format != null && format.showtimeCount() >= 3 && format.hasShowtimeContext()) {
            occupancy = weighted(occupancy, totalWeight, format.averageOccupancy(), new BigDecimal("0.75"));
            totalWeight = totalWeight.add(new BigDecimal("0.75"));
        }
        if (movie != null && movie.showtimeCount() >= 2 && movie.hasShowtimeContext()) {
            occupancy = weighted(occupancy, totalWeight, movie.averageOccupancy(), new BigDecimal("2.00"));
            totalWeight = totalWeight.add(new BigDecimal("2.00"));
        }

        BigDecimal multiplier = BigDecimal.ONE;
        if (weekend) multiplier = multiplier.multiply(new BigDecimal("1.08"));
        if (primeTime) multiplier = multiplier.multiply(new BigDecimal("1.12"));
        if (candidate.releaseDate() != null) {
            long releaseAge = ChronoUnit.DAYS.between(candidate.releaseDate(), candidate.serviceDate());
            if (releaseAge >= 0 && releaseAge <= 7) multiplier = multiplier.multiply(new BigDecimal("1.10"));
            else if (releaseAge > 28) multiplier = multiplier.multiply(new BigDecimal("0.92"));
        }
        DemandHistorySnapshot.Aggregate trendFacts = movie != null ? movie : cinema;
        if (trendFacts.previousTicketsPerDay().signum() > 0) {
            BigDecimal trend = trendFacts.recentTicketsPerDay().divide(
                    trendFacts.previousTicketsPerDay(), 6, RoundingMode.HALF_UP);
            multiplier = multiplier.multiply(clamp(trend, new BigDecimal("0.85"), new BigDecimal("1.15")));
        }
        if (candidate.existingSameMovieShowtimes() > 0) {
            BigDecimal cannibalization = BigDecimal.valueOf(Math.min(12,
                    candidate.existingSameMovieShowtimes())).multiply(new BigDecimal("0.015"));
            multiplier = multiplier.multiply(BigDecimal.ONE.subtract(cannibalization));
        }
        occupancy = clamp(occupancy.multiply(multiplier), new BigDecimal("0.05"), new BigDecimal("0.95"));

        BigDecimal attendance = occupancy.multiply(BigDecimal.valueOf(candidate.auditoriumCapacity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal revenue = attendance.multiply(candidate.ticketPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal refundRate = trendFacts.refundRate() == null ? BigDecimal.ZERO : trendFacts.refundRate();
        BigDecimal cancellationRate = trendFacts.cancellationRate() == null
                ? BigDecimal.ZERO : trendFacts.cancellationRate();
        BigDecimal contribution = revenue.multiply(BigDecimal.ONE.subtract(
                        clamp(refundRate.add(cancellationRate), BigDecimal.ZERO, new BigDecimal("0.50"))))
                .setScale(2, RoundingMode.HALF_UP);

        List<String> risks = new ArrayList<>();
        BigDecimal confidence;
        if (!history.sourceAvailable()) {
            confidence = new BigDecimal("0.20");
            risks.add("ANALYTICS_SOURCE_UNAVAILABLE");
            risks.add("COLD_START_EXPLORATION");
        } else if (movie == null || movie.showtimeCount() < 2) {
            confidence = cinema.hasShowtimeContext() ? new BigDecimal("0.42") : new BigDecimal("0.28");
            risks.add("MOVIE_COLD_START");
            risks.add("COLD_START_EXPLORATION");
        } else {
            confidence = BigDecimal.valueOf(Math.min(85, 50 + movie.showtimeCount() * 3L), 2);
        }
        if (history.factsWithShowtimeContext() < history.sourceBookingFactCount()) {
            risks.add("PARTIAL_SHOWTIME_HISTORY");
            confidence = confidence.multiply(new BigDecimal("0.90")).setScale(4, RoundingMode.HALF_UP);
        }

        String explanation = "Blend of cinema prior"
                + (slot == null ? ", model time-slot prior" : ", observed time-slot history")
                + (movie == null ? ", cold-start movie prior" : ", movie history")
                + (format == null ? ", format-neutral prior" : ", format history")
                + "; adjusted for weekend/prime-time, release freshness, velocity, refunds and cancellations.";
        return new DemandEstimate(attendance, occupancy.setScale(6, RoundingMode.HALF_UP),
                revenue, contribution, clamp(confidence, BigDecimal.ZERO, BigDecimal.ONE),
                explanation, MODEL_VERSION, primeTime, List.copyOf(risks));
    }

    private DemandHistorySnapshot.Aggregate safe(DemandHistorySnapshot.Aggregate value) {
        return value == null ? DemandHistorySnapshot.Aggregate.empty() : value;
    }

    private BigDecimal weighted(BigDecimal current, BigDecimal currentWeight,
                                BigDecimal next, BigDecimal nextWeight) {
        return current.multiply(currentWeight).add(next.multiply(nextWeight))
                .divide(currentWeight.add(nextWeight), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        if (value == null) return minimum;
        return value.max(minimum).min(maximum);
    }
}
