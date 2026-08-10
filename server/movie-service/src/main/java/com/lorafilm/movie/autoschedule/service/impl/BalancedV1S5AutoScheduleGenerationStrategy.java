package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * S5 keeps S4's score-optimal coverage result, then performs deterministic
 * quality-guarded substitutions so one eligible movie cannot consume nearly
 * every selected slot merely because it wins stable WIS tie-breaks or is shorter.
 */
@Component
public class BalancedV1S5AutoScheduleGenerationStrategy implements AutoScheduleGenerationStrategy {

    private static final BigDecimal QUALITY_RETENTION_NUMERATOR = new BigDecimal("90");
    private static final BigDecimal QUALITY_RETENTION_DENOMINATOR = new BigDecimal("100");
    private static final BigDecimal ZERO = new BigDecimal("0.000");

    private static final Comparator<MovieKey> MOVIE_ORDER = Comparator
            .comparing(MovieKey::publicId)
            .thenComparing(MovieKey::id);

    private static final Comparator<ShowtimeCandidate> CANDIDATE_ORDER = Comparator
            .comparing(ShowtimeCandidate::getStartTime)
            .thenComparing(ShowtimeCandidate::getOccupancyEndTime)
            .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
            .thenComparing(ShowtimeCandidate::getMovieVersionPublicId);

    private static final Comparator<DistributionSwap> SWAP_ORDER = Comparator
            .comparingInt(DistributionSwap::resultingImbalance)
            .thenComparing(DistributionSwap::qualityLoss)
            .thenComparingInt(swap -> swap.removed().size())
            .thenComparingInt(DistributionSwap::sameStartPenalty)
            .thenComparingLong(DistributionSwap::startDistanceSeconds)
            .thenComparing(DistributionSwap::replacement, CANDIDATE_ORDER)
            .thenComparing(DistributionSwap::removedSignature);

    private final BalancedV1S4AutoScheduleGenerationStrategy coverageStrategy;
    private final CandidateSelectionResolverImpl selectionValidator;

    public BalancedV1S5AutoScheduleGenerationStrategy(
            BalancedV1S4AutoScheduleGenerationStrategy coverageStrategy,
            CandidateSelectionResolverImpl selectionValidator) {
        this.coverageStrategy = coverageStrategy;
        this.selectionValidator = selectionValidator;
    }

    @Override
    public String getStrategyVersion() {
        return AutoScheduleStrategyVersions.BALANCED_V1_S5;
    }

    @Override
    public void scoreAndResolveDefaultSelection(List<ShowtimeCandidate> candidates,
                                                CandidateScoringContext scoringContext) {
        scoreAndResolveWithDiagnostics(candidates, scoringContext);
    }

    S5Diagnostics scoreAndResolveWithDiagnostics(List<ShowtimeCandidate> candidates,
                                                 CandidateScoringContext scoringContext) {
        coverageStrategy.scoreAndResolveDefaultSelection(candidates, scoringContext);

        Map<LocalDate, Integer> imbalanceBefore = imbalanceByDate(candidates);
        Map<LocalDate, Integer> swapsByDate = new LinkedHashMap<>();
        Map<LocalDate, Integer> imbalanceAfter = new LinkedHashMap<>();
        Set<LocalDate> dates = eligibleDates(candidates);
        for (LocalDate date : dates) {
            int swapCount = rebalanceDate(candidates, date);
            swapsByDate.put(date, swapCount);
            imbalanceAfter.put(date, distributionImbalance(selectedCounts(candidates, date)));
        }

        selectionValidator.validateGlobalSelectionInvariant(candidates);
        candidates.sort(Comparator.comparing(ShowtimeCandidate::getRankingPosition));
        return new S5Diagnostics(
                swapsByDate.values().stream().mapToInt(Integer::intValue).sum(),
                Map.copyOf(imbalanceBefore),
                Map.copyOf(imbalanceAfter),
                Map.copyOf(swapsByDate));
    }

    private int rebalanceDate(List<ShowtimeCandidate> candidates, LocalDate date) {
        Set<MovieKey> eligibleMovies = eligibleMovies(candidates, date);
        if (eligibleMovies.size() < 2) {
            return 0;
        }

        BigDecimal referenceScore = selectedBaseScore(candidates, date);
        BigDecimal qualityFloor = referenceScore.multiply(QUALITY_RETENTION_NUMERATOR)
                .divide(QUALITY_RETENTION_DENOMINATOR);
        DistributionQuality referenceQuality = selectedDistributionQuality(candidates, date);
        BigDecimal currentScore = referenceScore;
        int adjustments = 0;
        Map<MovieKey, Integer> initialCounts = selectedCounts(candidates, date);
        eligibleMovies.forEach(movie -> initialCounts.putIfAbsent(movie, 0));
        int initialImbalance = distributionImbalance(initialCounts);

        DistributionRound rebuilt = buildBalancedDistributionRound(candidates, date, eligibleMovies);
        if (meetsDistributionQualityFloor(rebuilt.quality(), referenceQuality)
                && rebuilt.imbalance() < initialImbalance) {
            adjustments += applyDistributionRound(candidates, date, rebuilt.selected());
            currentScore = rebuilt.quality().baseScoreTotal();
        }

        int swaps = 0;
        int maximumPasses = Math.max(1, candidates.size());

        while (swaps < maximumPasses) {
            Map<MovieKey, Integer> counts = selectedCounts(candidates, date);
            eligibleMovies.forEach(movie -> counts.putIfAbsent(movie, 0));
            int currentImbalance = distributionImbalance(counts);
            if (currentImbalance == 0) {
                break;
            }

            DistributionSwap best = findBestSwap(
                    candidates, date, counts, currentImbalance, currentScore, qualityFloor);
            if (best == null) {
                break;
            }

            best.removed().forEach(candidate -> candidate.setSelected(false));
            best.replacement().setSelected(true);
            currentScore = currentScore.subtract(best.removedScore())
                    .add(baseScore(best.replacement()));
            swaps++;
        }
        return adjustments + swaps;
    }

    private DistributionRound buildBalancedDistributionRound(
            List<ShowtimeCandidate> candidates,
            LocalDate date,
            Set<MovieKey> eligibleMovies) {
        Set<ShowtimeCandidate> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Long, List<ShowtimeCandidate>> occupiedByAuditorium = new HashMap<>();
        candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .filter(candidate -> !candidate.getOperatingWindow().getServiceDate().equals(date))
                .forEach(candidate -> occupiedByAuditorium
                        .computeIfAbsent(candidate.getAuditoriumId(), ignored -> new ArrayList<>())
                        .add(candidate));

        Map<MovieKey, Integer> counts = new TreeMap<>(MOVIE_ORDER);
        eligibleMovies.forEach(movie -> counts.put(movie, 0));
        List<ShowtimeCandidate> dateCandidates = candidates.stream()
                .filter(candidate -> isValidOnDate(candidate, date))
                .toList();

        while (true) {
            Comparator<ShowtimeCandidate> balancedOrder = Comparator
                    .comparingInt((ShowtimeCandidate candidate) ->
                            counts.getOrDefault(movieKey(candidate), 0))
                    .thenComparing(ShowtimeCandidate::getOccupancyEndTime)
                    .thenComparing(ShowtimeCandidate::getScore, Comparator.reverseOrder())
                    .thenComparing(CANDIDATE_ORDER);
            ShowtimeCandidate chosen = dateCandidates.stream()
                    .filter(candidate -> !selected.contains(candidate))
                    .filter(candidate -> occupiedByAuditorium
                            .getOrDefault(candidate.getAuditoriumId(), List.of())
                            .stream()
                            .noneMatch(existing -> overlaps(candidate, existing)))
                    .min(balancedOrder)
                    .orElse(null);
            if (chosen == null) {
                break;
            }
            selected.add(chosen);
            counts.merge(movieKey(chosen), 1, Integer::sum);
            occupiedByAuditorium
                    .computeIfAbsent(chosen.getAuditoriumId(), ignored -> new ArrayList<>())
                    .add(chosen);
        }

        DistributionQuality quality = distributionQuality(selected);
        return new DistributionRound(
                Set.copyOf(selected), quality, distributionImbalance(counts));
    }

    private int applyDistributionRound(List<ShowtimeCandidate> candidates,
                                       LocalDate date,
                                       Set<ShowtimeCandidate> selectedRound) {
        int added = 0;
        for (ShowtimeCandidate candidate : candidates) {
            if (!isValidOnDate(candidate, date)) {
                continue;
            }
            boolean shouldSelect = selectedRound.contains(candidate);
            if (shouldSelect && !candidate.isSelected()) {
                added++;
            }
            candidate.setSelected(shouldSelect);
        }
        return added;
    }

    private DistributionSwap findBestSwap(List<ShowtimeCandidate> candidates,
                                          LocalDate date,
                                          Map<MovieKey, Integer> currentCounts,
                                          int currentImbalance,
                                          BigDecimal currentScore,
                                          BigDecimal qualityFloor) {
        Map<Long, List<ShowtimeCandidate>> selectedByAuditorium = selectedByAuditorium(candidates, date);
        DistributionSwap best = null;

        List<ShowtimeCandidate> replacements = candidates.stream()
                .filter(candidate -> isValidOnDate(candidate, date))
                .filter(candidate -> !candidate.isSelected())
                .sorted(CANDIDATE_ORDER)
                .toList();

        for (ShowtimeCandidate replacement : replacements) {
            MovieKey replacementMovie = movieKey(replacement);
            int replacementCount = currentCounts.getOrDefault(replacementMovie, 0);
            List<ShowtimeCandidate> overlapping = selectedByAuditorium
                    .getOrDefault(replacement.getAuditoriumId(), List.of())
                    .stream()
                    .filter(selected -> overlaps(replacement, selected))
                    .toList();
            if (overlapping.isEmpty()
                    || overlapping.stream().anyMatch(selected -> movieKey(selected).equals(replacementMovie))) {
                continue;
            }

            boolean removesOnlyOverrepresentedMovies = overlapping.stream().allMatch(selected ->
                    currentCounts.getOrDefault(movieKey(selected), 0) > replacementCount + 1);
            if (!removesOnlyOverrepresentedMovies) {
                continue;
            }

            Map<MovieKey, Integer> projectedCounts = new HashMap<>(currentCounts);
            for (ShowtimeCandidate removed : overlapping) {
                projectedCounts.compute(movieKey(removed), (ignored, count) -> count - 1);
            }
            projectedCounts.merge(replacementMovie, 1, Integer::sum);
            int projectedImbalance = distributionImbalance(projectedCounts);
            if (projectedImbalance >= currentImbalance) {
                continue;
            }

            BigDecimal removedScore = overlapping.stream()
                    .map(this::baseScore)
                    .reduce(ZERO, BigDecimal::add);
            BigDecimal projectedScore = currentScore.subtract(removedScore)
                    .add(baseScore(replacement));
            if (projectedScore.compareTo(qualityFloor) < 0) {
                continue;
            }

            int sameStartPenalty = overlapping.stream()
                    .anyMatch(selected -> selected.getStartTime().equals(replacement.getStartTime()))
                    ? 0 : 1;
            long startDistance = overlapping.stream()
                    .mapToLong(selected -> Math.abs(Duration.between(
                            selected.getStartTime(), replacement.getStartTime()).getSeconds()))
                    .min()
                    .orElse(Long.MAX_VALUE);
            String removedSignature = overlapping.stream()
                    .sorted(CANDIDATE_ORDER)
                    .map(candidate -> candidate.getAuditoriumPublicId()
                            + "|" + candidate.getStartTime()
                            + "|" + candidate.getMovieVersionPublicId())
                    .reduce((left, right) -> left + ";" + right)
                    .orElse("");

            DistributionSwap swap = new DistributionSwap(
                    replacement,
                    List.copyOf(overlapping),
                    removedScore,
                    removedScore.subtract(baseScore(replacement)),
                    projectedImbalance,
                    sameStartPenalty,
                    startDistance,
                    removedSignature);
            if (best == null || SWAP_ORDER.compare(swap, best) < 0) {
                best = swap;
            }
        }
        return best;
    }

    private Map<LocalDate, Integer> imbalanceByDate(List<ShowtimeCandidate> candidates) {
        Map<LocalDate, Integer> result = new LinkedHashMap<>();
        for (LocalDate date : eligibleDates(candidates)) {
            Map<MovieKey, Integer> counts = selectedCounts(candidates, date);
            eligibleMovies(candidates, date).forEach(movie -> counts.putIfAbsent(movie, 0));
            result.put(date, distributionImbalance(counts));
        }
        return result;
    }

    private Set<LocalDate> eligibleDates(List<ShowtimeCandidate> candidates) {
        Set<LocalDate> dates = new TreeSet<>();
        candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .map(candidate -> candidate.getOperatingWindow().getServiceDate())
                .forEach(dates::add);
        return dates;
    }

    private Set<MovieKey> eligibleMovies(List<ShowtimeCandidate> candidates, LocalDate date) {
        Set<MovieKey> movies = new TreeSet<>(MOVIE_ORDER);
        candidates.stream()
                .filter(candidate -> isValidOnDate(candidate, date))
                .map(this::movieKey)
                .forEach(movies::add);
        return movies;
    }

    private Map<MovieKey, Integer> selectedCounts(List<ShowtimeCandidate> candidates, LocalDate date) {
        Map<MovieKey, Integer> counts = new TreeMap<>(MOVIE_ORDER);
        candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .filter(candidate -> candidate.getOperatingWindow().getServiceDate().equals(date))
                .forEach(candidate -> counts.merge(movieKey(candidate), 1, Integer::sum));
        return counts;
    }

    private Map<Long, List<ShowtimeCandidate>> selectedByAuditorium(
            List<ShowtimeCandidate> candidates,
            LocalDate date) {
        Map<Long, List<ShowtimeCandidate>> selected = new HashMap<>();
        candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .filter(candidate -> candidate.getOperatingWindow().getServiceDate().equals(date))
                .forEach(candidate -> selected
                        .computeIfAbsent(candidate.getAuditoriumId(), ignored -> new ArrayList<>())
                        .add(candidate));
        selected.values().forEach(list -> list.sort(CANDIDATE_ORDER));
        return selected;
    }

    private BigDecimal selectedBaseScore(List<ShowtimeCandidate> candidates, LocalDate date) {
        return candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .filter(candidate -> candidate.getOperatingWindow().getServiceDate().equals(date))
                .map(this::baseScore)
                .reduce(ZERO, BigDecimal::add);
    }

    private DistributionQuality selectedDistributionQuality(
            List<ShowtimeCandidate> candidates,
            LocalDate date) {
        Set<ShowtimeCandidate> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        candidates.stream()
                .filter(ShowtimeCandidate::isSelected)
                .filter(candidate -> candidate.getOperatingWindow().getServiceDate().equals(date))
                .forEach(selected::add);
        return distributionQuality(selected);
    }

    private DistributionQuality distributionQuality(Set<ShowtimeCandidate> selected) {
        BigDecimal total = selected.stream()
                .map(this::baseScore)
                .reduce(ZERO, BigDecimal::add);
        long occupancySeconds = selected.stream()
                .mapToLong(candidate -> Duration.between(
                        candidate.getStartTime(), candidate.getOccupancyEndTime()).getSeconds())
                .sum();
        return new DistributionQuality(total, selected.size(), occupancySeconds);
    }

    private boolean meetsDistributionQualityFloor(DistributionQuality proposed,
                                                  DistributionQuality reference) {
        if (reference.selectedCount() == 0) {
            return proposed.selectedCount() == 0;
        }
        if (proposed.selectedCount() == 0) {
            return false;
        }

        BigDecimal proposedAverageScaled = proposed.baseScoreTotal()
                .multiply(BigDecimal.valueOf(reference.selectedCount()))
                .multiply(QUALITY_RETENTION_DENOMINATOR);
        BigDecimal referenceAverageScaled = reference.baseScoreTotal()
                .multiply(BigDecimal.valueOf(proposed.selectedCount()))
                .multiply(QUALITY_RETENTION_NUMERATOR);
        boolean averageScoreRetained =
                proposedAverageScaled.compareTo(referenceAverageScaled) >= 0;
        boolean occupancyRetained =
                proposed.occupancySeconds() * QUALITY_RETENTION_DENOMINATOR.longValueExact()
                        >= reference.occupancySeconds()
                        * QUALITY_RETENTION_NUMERATOR.longValueExact();
        return averageScoreRetained && occupancyRetained;
    }

    private BigDecimal baseScore(ShowtimeCandidate candidate) {
        BigDecimal coverageAdjustment = candidate.getScoreBreakdown()
                .getOrDefault("coverageSearchAdjustment", ZERO);
        return candidate.getScore().subtract(coverageAdjustment);
    }

    private int distributionImbalance(Map<MovieKey, Integer> counts) {
        List<Integer> values = new ArrayList<>(counts.values());
        int imbalance = 0;
        for (int left = 0; left < values.size(); left++) {
            for (int right = left + 1; right < values.size(); right++) {
                imbalance += Math.abs(values.get(left) - values.get(right));
            }
        }
        return imbalance;
    }

    private boolean isValidOnDate(ShowtimeCandidate candidate, LocalDate date) {
        return candidate.getValidationStatus() == PreviewItemValidationStatus.VALID
                && candidate.getOperatingWindow().getServiceDate().equals(date);
    }

    private MovieKey movieKey(ShowtimeCandidate candidate) {
        return new MovieKey(
                candidate.getMovieVersionSnapshot().movieId(),
                candidate.getMovieVersionSnapshot().movie().publicId());
    }

    private boolean overlaps(ShowtimeCandidate first, ShowtimeCandidate second) {
        return first.getStartTime().isBefore(second.getOccupancyEndTime())
                && second.getStartTime().isBefore(first.getOccupancyEndTime());
    }

    record S5Diagnostics(int swapCount,
                         Map<LocalDate, Integer> imbalanceBefore,
                         Map<LocalDate, Integer> imbalanceAfter,
                         Map<LocalDate, Integer> swapsByDate) {
    }

    private record MovieKey(Long id, String publicId) {
    }

    private record DistributionSwap(ShowtimeCandidate replacement,
                                    List<ShowtimeCandidate> removed,
                                    BigDecimal removedScore,
                                    BigDecimal qualityLoss,
                                    int resultingImbalance,
                                    int sameStartPenalty,
                                    long startDistanceSeconds,
                                    String removedSignature) {
    }

    private record DistributionRound(Set<ShowtimeCandidate> selected,
                                     DistributionQuality quality,
                                     int imbalance) {
    }

    private record DistributionQuality(BigDecimal baseScoreTotal,
                                       int selectedCount,
                                       long occupancySeconds) {
    }
}
