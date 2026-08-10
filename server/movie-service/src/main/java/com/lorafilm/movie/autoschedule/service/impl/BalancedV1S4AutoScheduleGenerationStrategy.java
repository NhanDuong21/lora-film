package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.CandidateScoreResult;
import com.lorafilm.movie.autoschedule.model.CandidateScoringContext;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationStrategy;
import com.lorafilm.movie.autoschedule.service.BalancedCandidateScoringService;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/** Approved S4A coverage strategy. It is registered but remains non-current. */
@Component
public class BalancedV1S4AutoScheduleGenerationStrategy implements AutoScheduleGenerationStrategy {

    static final BigDecimal COVERAGE_SEARCH_ADJUSTMENT = new BigDecimal("20.000");
    static final BigDecimal LOCAL_OPPORTUNITY_LOSS_CEILING = new BigDecimal("15.000");
    private static final BigDecimal ZERO = new BigDecimal("0.000");
    private static final BigDecimal RETENTION_NUMERATOR = new BigDecimal("90");
    private static final BigDecimal RETENTION_DENOMINATOR = new BigDecimal("100");

    static final List<String> BREAKDOWN_KEYS = List.of(
            "base",
            "primeTime",
            "offPeak",
            "earlySlot",
            "auditoriumFit",
            "scheduleContinuity",
            "coverageSearchAdjustment"
    );

    private static final List<String> BASE_BREAKDOWN_KEYS = BREAKDOWN_KEYS.subList(0, 6);

    private static final Comparator<GroupKey> GROUP_ORDER = Comparator
            .comparing(GroupKey::serviceDate)
            .thenComparing(GroupKey::moviePublicId);

    private static final Comparator<ShowtimeCandidate> CANONICAL_CANDIDATE_ORDER = Comparator
            .comparing(ShowtimeCandidate::getOccupancyEndTime)
            .thenComparing(ShowtimeCandidate::getStartTime)
            .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
            .thenComparing(ShowtimeCandidate::getMovieVersionPublicId)
            .thenComparing(candidate -> candidate.getOperatingWindow().getServiceDate());

    private final BalancedCandidateScoringService scoringService;
    private final CandidateSelectionResolver selectionResolver;

    public BalancedV1S4AutoScheduleGenerationStrategy(BalancedCandidateScoringService scoringService,
                                                      CandidateSelectionResolver selectionResolver) {
        this.scoringService = scoringService;
        this.selectionResolver = selectionResolver;
    }

    @Override
    public String getStrategyVersion() {
        return AutoScheduleStrategyVersions.BALANCED_V1_S4;
    }

    @Override
    public void scoreAndResolveDefaultSelection(List<ShowtimeCandidate> candidates,
                                                CandidateScoringContext scoringContext) {
        execute(candidates, scoringContext, false);
    }

    S4Diagnostics scoreAndResolveWithDiagnostics(List<ShowtimeCandidate> candidates,
                                                 CandidateScoringContext scoringContext) {
        return execute(candidates, scoringContext, true);
    }

    private S4Diagnostics execute(List<ShowtimeCandidate> candidates,
                                  CandidateScoringContext scoringContext,
                                  boolean collectDiagnostics) {
        long totalStarted = System.nanoTime();
        AutoScheduleGenerationContext generationContext = scoringContext.getGenerationContext();
        Map<ShowtimeCandidate, BaseScoreState> baseScores = scoreBaseCandidates(candidates, scoringContext);

        applyEffectiveScores(candidates, baseScores, Set.of());
        long baselineWisStarted = System.nanoTime();
        selectionResolver.resolveDefaultSelection(candidates);
        long baselineWisNanos = System.nanoTime() - baselineWisStarted;
        RoundState baseline = RoundState.capture(candidates);

        Map<GroupKey, List<ShowtimeCandidate>> eligibleGroups = eligibleGroups(candidates);
        int existingCoveredGroups = countExistingCoveredGroups(eligibleGroups.keySet(), generationContext);
        int baselineCoveredGroups = countCoveredGroups(
                eligibleGroups.keySet(), generationContext, baseline);

        long anchorStarted = System.nanoTime();
        Set<ShowtimeCandidate> anchors = chooseCoverageAnchors(
                eligibleGroups, generationContext, baseline, baseScores);
        long anchorNanos = System.nanoTime() - anchorStarted;

        applyEffectiveScores(candidates, baseScores, anchors);
        long coverageWisStarted = System.nanoTime();
        selectionResolver.resolveDefaultSelection(candidates);
        long coverageWisNanos = System.nanoTime() - coverageWisStarted;
        RoundState coverage = RoundState.capture(candidates);

        boolean coverageAdmissible = meetsAggregateQualityFloor(
                candidates, baseline, coverage, baseScores)
                && anchorsMeetLocalLossCeiling(anchors, baseline, baseScores);
        int coverageCoveredGroups = countCoveredGroups(
                eligibleGroups.keySet(), generationContext, coverage);

        long comparatorStarted = System.nanoTime();
        RoundState winner = coverageAdmissible && coverageWinsOuterComparator(
                candidates, eligibleGroups.keySet(), generationContext,
                baseline, coverage, baseScores)
                ? coverage
                : baseline;
        long comparatorNanos = System.nanoTime() - comparatorStarted;
        winner.apply(candidates);

        return new S4Diagnostics(
                candidates.size(), eligibleGroups.size(), existingCoveredGroups,
                baselineCoveredGroups, anchors.size(), coverageCoveredGroups,
                coverageAdmissible, winner == coverage,
                collectDiagnostics ? baselineWisNanos : 0L,
                collectDiagnostics ? anchorNanos : 0L,
                collectDiagnostics ? coverageWisNanos : 0L,
                collectDiagnostics ? comparatorNanos : 0L,
                collectDiagnostics ? System.nanoTime() - totalStarted : 0L);
    }

    private Map<ShowtimeCandidate, BaseScoreState> scoreBaseCandidates(
            List<ShowtimeCandidate> candidates,
            CandidateScoringContext context) {
        Map<ShowtimeCandidate, BaseScoreState> result = new IdentityHashMap<>();
        for (ShowtimeCandidate candidate : candidates) {
            CandidateScoreResult scored = scoringService.score(candidate, context);
            if (candidate.getValidationStatus() == PreviewItemValidationStatus.REJECTED) {
                result.put(candidate, new BaseScoreState(ZERO, Map.of()));
                continue;
            }

            LinkedHashMap<String, BigDecimal> canonical = new LinkedHashMap<>();
            for (String key : BASE_BREAKDOWN_KEYS) {
                BigDecimal value = scored.getScoreBreakdown().get(key);
                if (value == null) {
                    throw new IllegalStateException("S3 base breakdown is missing key " + key);
                }
                canonical.put(key, scale(value));
            }
            BigDecimal baseScore = canonical.values().stream().reduce(ZERO, BigDecimal::add);
            if (baseScore.compareTo(scale(scored.getScore())) != 0) {
                throw new IllegalStateException("S3 base breakdown does not sum to its score");
            }
            result.put(candidate, new BaseScoreState(
                    baseScore, Collections.unmodifiableMap(canonical)));
        }
        return result;
    }

    private void applyEffectiveScores(List<ShowtimeCandidate> candidates,
                                      Map<ShowtimeCandidate, BaseScoreState> baseScores,
                                      Set<ShowtimeCandidate> anchors) {
        for (ShowtimeCandidate candidate : candidates) {
            if (candidate.getValidationStatus() == PreviewItemValidationStatus.REJECTED) {
                candidate.setScore(ZERO);
                candidate.setScoreBreakdown(Map.of());
                continue;
            }
            BaseScoreState base = requireBaseScore(baseScores, candidate);
            BigDecimal adjustment = anchors.contains(candidate) ? COVERAGE_SEARCH_ADJUSTMENT : ZERO;
            LinkedHashMap<String, BigDecimal> breakdown = new LinkedHashMap<>();
            BASE_BREAKDOWN_KEYS.forEach(key -> breakdown.put(key, base.breakdown().get(key)));
            breakdown.put("coverageSearchAdjustment", adjustment);
            candidate.setScore(scale(base.score().add(adjustment)));
            candidate.setScoreBreakdown(Collections.unmodifiableMap(breakdown));
        }
    }

    private Map<GroupKey, List<ShowtimeCandidate>> eligibleGroups(List<ShowtimeCandidate> candidates) {
        Map<GroupKey, List<ShowtimeCandidate>> groups = new TreeMap<>(GROUP_ORDER);
        for (ShowtimeCandidate candidate : candidates) {
            if (candidate.getValidationStatus() != PreviewItemValidationStatus.VALID) {
                continue;
            }
            AutoScheduleGenerationContext.MovieVersionSnapshot version =
                    candidate.getMovieVersionSnapshot();
            GroupKey key = new GroupKey(
                    candidate.getOperatingWindow().getServiceDate(),
                    version.movieId(),
                    version.movie().publicId());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
        }
        groups.values().forEach(group -> group.sort(CANONICAL_CANDIDATE_ORDER));
        return groups;
    }

    private Set<ShowtimeCandidate> chooseCoverageAnchors(
            Map<GroupKey, List<ShowtimeCandidate>> eligibleGroups,
            AutoScheduleGenerationContext context,
            RoundState baseline,
            Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        BaselineOverlapIndex overlapIndex = BaselineOverlapIndex.build(
                baseline.selectedCandidates(), baseScores);
        List<GroupAnchorOptions> uncovered = new ArrayList<>();
        for (Map.Entry<GroupKey, List<ShowtimeCandidate>> entry : eligibleGroups.entrySet()) {
            GroupKey group = entry.getKey();
            if (isExistingCovered(group, context) || baseline.covers(group)) {
                continue;
            }
            List<AnchorOption> options = entry.getValue().stream()
                    .map(candidate -> new AnchorOption(
                            candidate,
                            opportunityLoss(candidate, overlapIndex, baseScores)))
                    .sorted(anchorOptionOrder(baseScores))
                    .toList();
            if (!options.isEmpty()) {
                uncovered.add(new GroupAnchorOptions(group, options, options.getFirst().loss()));
            }
        }
        uncovered.sort(Comparator.comparing(GroupAnchorOptions::bestLoss)
                .thenComparing(GroupAnchorOptions::group, GROUP_ORDER));

        Set<ShowtimeCandidate> anchors = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<Long, List<ShowtimeCandidate>> chosenByAuditorium = new HashMap<>();
        for (GroupAnchorOptions group : uncovered) {
            List<AnchorOption> qualified = group.options().stream()
                    .filter(option -> option.loss().compareTo(LOCAL_OPPORTUNITY_LOSS_CEILING) <= 0)
                    .toList();
            if (qualified.isEmpty()) {
                continue;
            }
            AnchorOption chosen = qualified.stream()
                    .filter(option -> doesNotOverlapChosen(
                            option.candidate(), chosenByAuditorium.getOrDefault(
                                    option.candidate().getAuditoriumId(), List.of())))
                    .findFirst()
                    .orElse(qualified.getFirst());
            anchors.add(chosen.candidate());
            chosenByAuditorium.computeIfAbsent(
                    chosen.candidate().getAuditoriumId(), ignored -> new ArrayList<>())
                    .add(chosen.candidate());
        }
        return anchors;
    }

    private Comparator<AnchorOption> anchorOptionOrder(
            Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        return Comparator.comparing(AnchorOption::loss)
                .thenComparing(option -> requireBaseScore(
                        baseScores, option.candidate()).score(), Comparator.reverseOrder())
                .thenComparing(AnchorOption::candidate, CANONICAL_CANDIDATE_ORDER);
    }

    private BigDecimal opportunityLoss(ShowtimeCandidate anchor,
                                       BaselineOverlapIndex overlapIndex,
                                       Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        BigDecimal displaced = overlapIndex.overlappingBaseScore(anchor);
        BigDecimal loss = displaced.subtract(requireBaseScore(baseScores, anchor).score());
        return loss.signum() < 0 ? ZERO : scale(loss);
    }

    private boolean anchorsMeetLocalLossCeiling(Set<ShowtimeCandidate> anchors,
                                                RoundState baseline,
                                                Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        BaselineOverlapIndex index = BaselineOverlapIndex.build(
                baseline.selectedCandidates(), baseScores);
        return anchors.stream().allMatch(anchor -> opportunityLoss(anchor, index, baseScores)
                .compareTo(LOCAL_OPPORTUNITY_LOSS_CEILING) <= 0);
    }

    private boolean meetsAggregateQualityFloor(List<ShowtimeCandidate> candidates,
                                               RoundState baseline,
                                               RoundState coverage,
                                               Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        Set<LocalDate> dates = new TreeSet<>();
        candidates.stream()
                .filter(candidate -> candidate.getValidationStatus() == PreviewItemValidationStatus.VALID)
                .map(candidate -> candidate.getOperatingWindow().getServiceDate())
                .forEach(dates::add);
        Map<LocalDate, BigDecimal> baselineTotals = selectedBaseTotalsByDate(baseline, baseScores);
        Map<LocalDate, BigDecimal> coverageTotals = selectedBaseTotalsByDate(coverage, baseScores);
        for (LocalDate date : dates) {
            BigDecimal baselineTotal = baselineTotals.getOrDefault(date, ZERO);
            BigDecimal coverageTotal = coverageTotals.getOrDefault(date, ZERO);
            if (coverageTotal.multiply(RETENTION_DENOMINATOR)
                    .compareTo(baselineTotal.multiply(RETENTION_NUMERATOR)) < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean coverageWinsOuterComparator(
            List<ShowtimeCandidate> candidates,
            Set<GroupKey> eligibleGroups,
            AutoScheduleGenerationContext context,
            RoundState baseline,
            RoundState coverage,
            Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        int baselineCovered = countCoveredGroups(eligibleGroups, context, baseline);
        int coverageCovered = countCoveredGroups(eligibleGroups, context, coverage);
        if (coverageCovered != baselineCovered) {
            return coverageCovered > baselineCovered;
        }

        int baseComparison = selectedBaseTotal(coverage, baseScores)
                .compareTo(selectedBaseTotal(baseline, baseScores));
        if (baseComparison != 0) {
            return baseComparison > 0;
        }

        List<ShowtimeCandidate> canonical = new ArrayList<>(candidates);
        canonical.sort(CANONICAL_CANDIDATE_ORDER);
        for (ShowtimeCandidate candidate : canonical) {
            boolean coverageSelected = coverage.isSelected(candidate);
            boolean baselineSelected = baseline.isSelected(candidate);
            if (coverageSelected != baselineSelected) {
                return !coverageSelected;
            }
        }
        return false;
    }

    private int countExistingCoveredGroups(Set<GroupKey> groups,
                                           AutoScheduleGenerationContext context) {
        return Math.toIntExact(groups.stream().filter(group -> isExistingCovered(group, context)).count());
    }

    private int countCoveredGroups(Set<GroupKey> groups,
                                   AutoScheduleGenerationContext context,
                                   RoundState state) {
        return Math.toIntExact(groups.stream()
                .filter(group -> isExistingCovered(group, context) || state.covers(group))
                .count());
    }

    private boolean isExistingCovered(GroupKey group, AutoScheduleGenerationContext context) {
        return context.existingShowtimeCount(group.serviceDate(), group.movieId()) > 0;
    }

    private Map<LocalDate, BigDecimal> selectedBaseTotalsByDate(
            RoundState state,
            Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        Map<LocalDate, BigDecimal> totals = new HashMap<>();
        for (ShowtimeCandidate candidate : state.selectedCandidates()) {
            totals.merge(candidate.getOperatingWindow().getServiceDate(),
                    requireBaseScore(baseScores, candidate).score(), BigDecimal::add);
        }
        return totals;
    }

    private BigDecimal selectedBaseTotal(RoundState state,
                                         Map<ShowtimeCandidate, BaseScoreState> baseScores) {
        return state.selectedCandidates().stream()
                .map(candidate -> requireBaseScore(baseScores, candidate).score())
                .reduce(ZERO, BigDecimal::add);
    }

    private boolean doesNotOverlapChosen(ShowtimeCandidate candidate,
                                         List<ShowtimeCandidate> chosen) {
        return chosen.stream().noneMatch(existing -> overlaps(candidate, existing));
    }

    private static boolean overlaps(ShowtimeCandidate first, ShowtimeCandidate second) {
        return first.getStartTime().isBefore(second.getOccupancyEndTime())
                && second.getStartTime().isBefore(first.getOccupancyEndTime());
    }

    private static BaseScoreState requireBaseScore(
            Map<ShowtimeCandidate, BaseScoreState> scores,
            ShowtimeCandidate candidate) {
        BaseScoreState state = scores.get(candidate);
        if (state == null) {
            throw new IllegalStateException("Missing S4 base score state");
        }
        return state;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    record S4Diagnostics(int candidateCount,
                         int eligibleGroupCount,
                         int existingCoveredGroupCount,
                         int baselineCoveredGroupCount,
                         int anchorCount,
                         int coverageRoundCoveredGroupCount,
                         boolean coverageRoundAdmissible,
                         boolean coverageRoundWon,
                         long baselineWisNanos,
                         long anchorNanos,
                         long coverageWisNanos,
                         long comparatorNanos,
                         long totalNanos) {
    }

    private record GroupKey(LocalDate serviceDate, Long movieId, String moviePublicId) {
    }

    private record BaseScoreState(BigDecimal score, Map<String, BigDecimal> breakdown) {
    }

    private record AnchorOption(ShowtimeCandidate candidate, BigDecimal loss) {
    }

    private record GroupAnchorOptions(GroupKey group,
                                      List<AnchorOption> options,
                                      BigDecimal bestLoss) {
    }

    private record CandidateRoundState(BigDecimal score,
                                       Map<String, BigDecimal> breakdown,
                                       Integer rankingPosition,
                                       boolean selected) {
    }

    private static final class RoundState {
        private final Map<ShowtimeCandidate, CandidateRoundState> candidates;
        private final Set<AutoScheduleGenerationContext.MovieServiceDateKey> selectedGroups;

        private RoundState(Map<ShowtimeCandidate, CandidateRoundState> candidates,
                           Set<AutoScheduleGenerationContext.MovieServiceDateKey> selectedGroups) {
            this.candidates = candidates;
            this.selectedGroups = selectedGroups;
        }

        static RoundState capture(List<ShowtimeCandidate> source) {
            Map<ShowtimeCandidate, CandidateRoundState> captured = new IdentityHashMap<>();
            Set<AutoScheduleGenerationContext.MovieServiceDateKey> selectedGroups = new LinkedHashSet<>();
            for (ShowtimeCandidate candidate : source) {
                captured.put(candidate, new CandidateRoundState(
                        candidate.getScore(), candidate.getScoreBreakdown(),
                        candidate.getRankingPosition(), candidate.isSelected()));
                if (candidate.isSelected()) {
                    selectedGroups.add(new AutoScheduleGenerationContext.MovieServiceDateKey(
                            candidate.getOperatingWindow().getServiceDate(),
                            candidate.getMovieVersionSnapshot().movieId()));
                }
            }
            return new RoundState(captured, Set.copyOf(selectedGroups));
        }

        void apply(List<ShowtimeCandidate> target) {
            for (ShowtimeCandidate candidate : target) {
                CandidateRoundState state = candidates.get(candidate);
                if (state == null) {
                    throw new IllegalStateException("Missing S4 round candidate state");
                }
                candidate.setScore(state.score());
                candidate.setScoreBreakdown(state.breakdown());
                candidate.setRankingPosition(state.rankingPosition());
                candidate.setSelected(state.selected());
            }
            target.sort(Comparator.comparing(ShowtimeCandidate::getRankingPosition));
        }

        boolean isSelected(ShowtimeCandidate candidate) {
            CandidateRoundState state = candidates.get(candidate);
            return state != null && state.selected();
        }

        boolean covers(GroupKey group) {
            return selectedGroups.contains(new AutoScheduleGenerationContext.MovieServiceDateKey(
                    group.serviceDate(), group.movieId()));
        }

        Set<ShowtimeCandidate> selectedCandidates() {
            Set<ShowtimeCandidate> selected = Collections.newSetFromMap(new IdentityHashMap<>());
            candidates.forEach((candidate, state) -> {
                if (state.selected()) {
                    selected.add(candidate);
                }
            });
            return selected;
        }
    }

    private static final class BaselineOverlapIndex {
        private final Map<Long, AuditoriumIndex> byAuditorium;

        private BaselineOverlapIndex(Map<Long, AuditoriumIndex> byAuditorium) {
            this.byAuditorium = byAuditorium;
        }

        static BaselineOverlapIndex build(
                Set<ShowtimeCandidate> selected,
                Map<ShowtimeCandidate, BaseScoreState> baseScores) {
            Map<Long, List<ShowtimeCandidate>> grouped = new HashMap<>();
            for (ShowtimeCandidate candidate : selected) {
                grouped.computeIfAbsent(candidate.getAuditoriumId(), ignored -> new ArrayList<>())
                        .add(candidate);
            }
            Map<Long, AuditoriumIndex> indexes = new HashMap<>();
            grouped.forEach((auditoriumId, candidates) -> {
                candidates.sort(Comparator.comparing(ShowtimeCandidate::getStartTime)
                        .thenComparing(ShowtimeCandidate::getOccupancyEndTime)
                        .thenComparing(CANONICAL_CANDIDATE_ORDER));
                List<Instant> starts = new ArrayList<>(candidates.size());
                List<Instant> ends = new ArrayList<>(candidates.size());
                BigDecimal[] prefix = new BigDecimal[candidates.size() + 1];
                prefix[0] = ZERO;
                for (int i = 0; i < candidates.size(); i++) {
                    ShowtimeCandidate candidate = candidates.get(i);
                    starts.add(candidate.getStartTime());
                    ends.add(candidate.getOccupancyEndTime());
                    prefix[i + 1] = prefix[i].add(requireBaseScore(baseScores, candidate).score());
                }
                indexes.put(auditoriumId, new AuditoriumIndex(starts, ends, prefix));
            });
            return new BaselineOverlapIndex(indexes);
        }

        BigDecimal overlappingBaseScore(ShowtimeCandidate anchor) {
            AuditoriumIndex index = byAuditorium.get(anchor.getAuditoriumId());
            return index == null ? ZERO : index.overlappingBaseScore(
                    anchor.getStartTime(), anchor.getOccupancyEndTime());
        }
    }

    private record AuditoriumIndex(List<Instant> starts,
                                   List<Instant> ends,
                                   BigDecimal[] prefixScores) {
        BigDecimal overlappingBaseScore(Instant start, Instant end) {
            int left = firstEndAfter(start);
            int right = firstStartAtOrAfter(end);
            if (right <= left) {
                return ZERO;
            }
            return prefixScores[right].subtract(prefixScores[left]);
        }

        private int firstEndAfter(Instant instant) {
            int low = 0;
            int high = ends.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (ends.get(middle).isAfter(instant)) {
                    high = middle;
                } else {
                    low = middle + 1;
                }
            }
            return low;
        }

        private int firstStartAtOrAfter(Instant instant) {
            int low = 0;
            int high = starts.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (starts.get(middle).isBefore(instant)) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }
    }
}
