package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidatePricingSnapshot;
import com.lorafilm.movie.autoschedule.model.DemandCandidateFacts;
import com.lorafilm.movie.autoschedule.model.DemandEstimate;
import com.lorafilm.movie.autoschedule.model.DemandHistorySnapshot;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.AutoScheduleCandidateEnrichmentService;
import com.lorafilm.movie.autoschedule.service.DemandEngine;
import com.lorafilm.movie.autoschedule.service.DemandHistoryProvider;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AutoScheduleCandidateEnrichmentServiceImpl implements AutoScheduleCandidateEnrichmentService {

    private static final int HISTORY_DAYS = 90;

    private final PricePolicyResolver pricePolicyResolver;
    private final SeatRepository seatRepository;
    private final DemandHistoryProvider demandHistoryProvider;
    private final DemandEngine demandEngine;
    private AutoScheduleMetrics metrics = AutoScheduleMetrics.noop();

    public AutoScheduleCandidateEnrichmentServiceImpl(PricePolicyResolver pricePolicyResolver,
                                                       SeatRepository seatRepository,
                                                       DemandHistoryProvider demandHistoryProvider,
                                                       DemandEngine demandEngine) {
        this.pricePolicyResolver = pricePolicyResolver;
        this.seatRepository = seatRepository;
        this.demandHistoryProvider = demandHistoryProvider;
        this.demandEngine = demandEngine;
    }

    @Autowired
    void setMetrics(AutoScheduleMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void enrich(List<ShowtimeCandidate> candidates, AutoScheduleGenerationContext context) {
        List<ShowtimeCandidate> valid = candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .toList();
        if (valid.isEmpty()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_NO_FEASIBLE_CANDIDATES);
        }

        List<Showtime> pricingProbes = valid.stream().map(this::pricingProbe).toList();
        List<PriceResolutionResult> resolutions = pricePolicyResolver.resolveAll(pricingProbes);
        Map<SeatCountKey, Long> seatCounts = loadSeatCounts(valid);
        for (int index = 0; index < valid.size(); index++) {
            ShowtimeCandidate candidate = valid.get(index);
            PriceResolutionResult resolution = resolutions.get(index);
            if (!resolution.isComplete() || resolution.resolvedPrices().isEmpty()) {
                throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREFLIGHT_BLOCKED,
                        "Pricing changed after preflight; run preflight again");
            }
            candidate.setPricingSnapshot(toPricingSnapshot(candidate, resolution, seatCounts));
        }

        LocalDate historyTo = context.getScheduleFrom().minusDays(1);
        LocalDate historyFrom = historyTo.minusDays(HISTORY_DAYS - 1L);
        DemandHistorySnapshot history = demandHistoryProvider.load(
                context.getCinema().publicId(), context.getCinema().zoneId(), historyFrom, historyTo,
                context.getMovieVersions().stream().map(item -> item.movie().publicId())
                        .distinct().sorted().toList());
        metrics.recordDemandHistory(history.cinemaPrior().averageOccupancy(),
                history.cinemaPrior().cancellationRate());
        for (ShowtimeCandidate candidate : valid) {
            DemandEstimate estimate = demandEngine.estimate(
                    new DemandCandidateFacts(
                            candidate.getMovie().getPublicId(), candidate.getFormat(),
                            candidate.getAuditoriumCapacity(), candidate.getStartTime(),
                            candidate.getServiceDate(), candidate.getMovie().getReleaseDate(),
                            context.getCinema().zoneId(),
                            candidate.getPricingSnapshot().weightedAverageTicketPrice(),
                            context.existingShowtimeCount(
                                    candidate.getServiceDate(), candidate.getMovie().getId())),
                    history);
            observedOccupancy(history, candidate.getMovie().getPublicId())
                    .ifPresent(observed -> metrics.recordHistoricalForecastError(
                            estimate.expectedOccupancy(), observed));
            candidate.setExpectedAttendance(estimate.expectedAttendance());
            candidate.setExpectedOccupancy(estimate.expectedOccupancy());
            candidate.setExpectedRevenue(estimate.expectedRevenue());
            candidate.setExpectedContribution(estimate.expectedContribution());
            candidate.setDemandConfidence(estimate.confidence());
            candidate.setDemandExplanation(estimate.explanation());
            candidate.setDemandModelVersion(estimate.demandModelVersion());
            candidate.setPrimeTime(estimate.primeTime());
            candidate.setRiskFlags(estimate.riskFlags());
        }
    }

    private java.util.Optional<BigDecimal> observedOccupancy(
            DemandHistorySnapshot history, String moviePublicId) {
        if (!history.sourceAvailable()) return java.util.Optional.empty();
        DemandHistorySnapshot.Aggregate movie = history.movies().stream()
                .filter(item -> moviePublicId.equals(item.moviePublicId()))
                .map(DemandHistorySnapshot.MovieHistory::aggregate)
                .filter(DemandHistorySnapshot.Aggregate::hasShowtimeContext)
                .findFirst().orElse(null);
        DemandHistorySnapshot.Aggregate aggregate = movie == null ? history.cinemaPrior() : movie;
        return aggregate != null && aggregate.hasShowtimeContext()
                ? java.util.Optional.ofNullable(aggregate.averageOccupancy())
                : java.util.Optional.empty();
    }

    private Showtime pricingProbe(ShowtimeCandidate candidate) {
        Showtime showtime = new Showtime();
        showtime.setMovie(candidate.getMovie());
        showtime.setMovieVersion(candidate.getMovieVersion());
        showtime.setCinema(candidate.getCinema());
        showtime.setAuditorium(candidate.getAuditorium());
        showtime.setStartTime(candidate.getStartTime());
        showtime.setEndTime(candidate.getEndTime());
        showtime.setServiceDate(candidate.getServiceDate());
        return showtime;
    }

    private Map<SeatCountKey, Long> loadSeatCounts(List<ShowtimeCandidate> candidates) {
        List<Long> auditoriumIds = candidates.stream().map(ShowtimeCandidate::getAuditoriumId)
                .distinct().sorted().toList();
        Map<SeatCountKey, Long> result = new HashMap<>();
        for (Object[] row : seatRepository.countActiveSeatsByAuditoriumAndSeatType(auditoriumIds)) {
            result.put(new SeatCountKey((Long) row[0], (Long) row[1]), ((Number) row[2]).longValue());
        }
        return result;
    }

    private CandidatePricingSnapshot toPricingSnapshot(
            ShowtimeCandidate candidate,
            PriceResolutionResult resolution,
            Map<SeatCountKey, Long> seatCounts) {
        List<CandidatePricingSnapshot.PriceLine> lines = new ArrayList<>();
        BigDecimal weightedTotal = BigDecimal.ZERO;
        long totalSeats = 0;
        for (PriceResolutionResult.ResolvedPrice price : resolution.resolvedPrices()) {
            long seatCount = seatCounts.getOrDefault(
                    new SeatCountKey(candidate.getAuditoriumId(), price.seatType().getId()), 0L);
            if (seatCount <= 0) continue;
            totalSeats += seatCount;
            weightedTotal = weightedTotal.add(price.price().multiply(BigDecimal.valueOf(seatCount)));
            lines.add(new CandidatePricingSnapshot.PriceLine(
                    price.seatType().getPublicId(), price.seatType().getCode().name(),
                    price.price(), seatCount, price.policy().getPublicId(), price.rule().getPublicId()));
        }
        if (totalSeats <= 0 || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.PRICING_INCOMPLETE,
                    "Auditorium has no active priced seats");
        }
        lines.sort(Comparator.comparing(CandidatePricingSnapshot.PriceLine::seatTypePublicId));
        BigDecimal average = weightedTotal.divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP);
        String canonical = resolution.currency() + "|" + resolution.timezone() + "|"
                + candidate.getAuditoriumPublicId() + "|" + candidate.getStartTime() + "|" + lines;
        return new CandidatePricingSnapshot(resolution.currency(), resolution.timezone(),
                resolution.resolvedAt(), average, List.copyOf(lines), sha256(canonical));
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record SeatCountKey(Long auditoriumId, Long seatTypeId) {
    }
}
