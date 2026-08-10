package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
public class AutoScheduleShadowComparisonService {

    private static final Logger log = LoggerFactory.getLogger(AutoScheduleShadowComparisonService.class);

    private final boolean enabled;
    private final BalancedV1S5AutoScheduleGenerationStrategy legacyStrategy;
    private final AutoScheduleMetrics metrics;

    public AutoScheduleShadowComparisonService(
            @Value("${autoschedule.shadow.s5-enabled:false}") boolean enabled,
            BalancedV1S5AutoScheduleGenerationStrategy legacyStrategy,
            AutoScheduleMetrics metrics) {
        this.enabled = enabled;
        this.legacyStrategy = legacyStrategy;
        this.metrics = metrics;
    }

    public void compareIfEnabled(String primaryStrategy,
                                 List<ShowtimeCandidate> primaryCandidates,
                                 AutoScheduleGenerationContext context) {
        if (!enabled || !AutoScheduleStrategyVersions.DEMAND_CP_SAT_V1.equals(primaryStrategy)) return;
        List<ShowtimeCandidate> shadow = primaryCandidates.stream()
                .map(ShowtimeCandidate::copyForReadOnlyComparison).toList();
        long started = System.nanoTime();
        try {
            legacyStrategy.scoreAndResolveDefaultSelection(shadow, new CandidateScoringContext(context));
            Summary primary = summarize(primaryCandidates);
            Summary legacy = summarize(shadow);
            int selectedDelta = primary.selectedCount() - legacy.selectedCount();
            BigDecimal contributionDelta = primary.contribution().subtract(legacy.contribution());
            BigDecimal occupancyDelta = primary.occupancy().subtract(legacy.occupancy());
            metrics.recordShadow("success", selectedDelta, contributionDelta, occupancyDelta,
                    primary.primeTimeCount() - legacy.primeTimeCount(),
                    primary.auditoriumUse() - legacy.auditoriumUse(),
                    Duration.ofNanos(System.nanoTime() - started), 0);
            log.info("Auto schedule shadow comparison completed. primaryEngine=cp_sat, shadowEngine=legacy_s5, "
                            + "selectedDelta={}, contributionDelta={}, occupancyDelta={}, "
                            + "primaryPrimeTime={}, shadowPrimeTime={}, primaryAuditoriumUse={}, shadowAuditoriumUse={}",
                    selectedDelta, contributionDelta, occupancyDelta,
                    primary.primeTimeCount(), legacy.primeTimeCount(),
                    primary.auditoriumUse(), legacy.auditoriumUse());
        } catch (RuntimeException exception) {
            metrics.recordShadow("failed", 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    0, 0, Duration.ofNanos(System.nanoTime() - started), 0);
            log.warn("Auto schedule S5 shadow failed without affecting the CP-SAT preview. errorType={}",
                    exception.getClass().getSimpleName());
        }
    }

    private Summary summarize(List<ShowtimeCandidate> candidates) {
        List<ShowtimeCandidate> selected = candidates.stream().filter(ShowtimeCandidate::isSelected).toList();
        BigDecimal contribution = selected.stream().map(ShowtimeCandidate::getExpectedContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal occupancy = selected.isEmpty() ? BigDecimal.ZERO
                : selected.stream().map(ShowtimeCandidate::getExpectedOccupancy)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(selected.size()), 6, RoundingMode.HALF_UP);
        long primeTime = selected.stream().filter(ShowtimeCandidate::isPrimeTime).count();
        long auditoriumUse = selected.stream().map(item -> item.getAuditoriumPublicId()
                + "|" + item.getServiceDate()).distinct().count();
        return new Summary(selected.size(), contribution, occupancy, primeTime, auditoriumUse);
    }

    private record Summary(int selectedCount, BigDecimal contribution, BigDecimal occupancy,
                           long primeTimeCount, long auditoriumUse) {
    }
}
