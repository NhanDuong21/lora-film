package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import com.lorafilm.movie.autoschedule.service.CandidateSelectionResolver;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CandidateSelectionResolverImpl implements CandidateSelectionResolver {

    private static final BigDecimal ZERO_SCORE = BigDecimal.ZERO.setScale(3);

    /** Phase S2 display ranking contract. Selection order is intentionally independent. */
    private static final Comparator<ShowtimeCandidate> GLOBAL_RANKING_COMPARATOR =
            Comparator.comparing((ShowtimeCandidate candidate) ->
                            candidate.getValidationStatus() == PreviewItemValidationStatus.VALID ? 0 : 1)
                    .thenComparing(candidate -> candidate.getScore() != null
                                    ? candidate.getScore() : BigDecimal.ZERO,
                            Comparator.reverseOrder())
                    .thenComparing(ShowtimeCandidate::getStartTime)
                    .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
                    .thenComparing(ShowtimeCandidate::getMovieVersionPublicId)
                    .thenComparing(candidate -> candidate.getRejectionCode() != null
                            ? candidate.getRejectionCode() : "");

    /**
     * End-first WIS order. Under the S2 unique candidate key, these stable business fields form a total order.
     */
    private static final Comparator<ShowtimeCandidate> WIS_CANDIDATE_COMPARATOR =
            Comparator.comparing(ShowtimeCandidate::getOccupancyEndTime)
                    .thenComparing(ShowtimeCandidate::getStartTime)
                    .thenComparing(ShowtimeCandidate::getAuditoriumPublicId)
                    .thenComparing(ShowtimeCandidate::getMovieVersionPublicId)
                    .thenComparing(candidate -> candidate.getOperatingWindow().getServiceDate());

    private static final Comparator<LogicalPartition> LOGICAL_PARTITION_COMPARATOR =
            Comparator.comparing(LogicalPartition::minimumStart)
                    .thenComparing(LogicalPartition::serviceDate)
                    .thenComparing(LogicalPartition::maximumOccupancyEnd);

    @Override
    public void resolveDefaultSelection(List<ShowtimeCandidate> candidates) {
        requireWellFormedCandidates(candidates);
        rankAndClearSelection(candidates);
        PartitioningResult partitioning = buildSafeOptimizationComponents(candidates);
        for (List<ShowtimeCandidate> component : partitioning.components()) {
            optimizeComponent(component);
        }
        validateGlobalSelectionInvariant(candidates);
    }

    /** Test-only instrumented entry point; the production interface path does not collect diagnostics. */
    SelectionDiagnostics resolveDefaultSelectionWithDiagnostics(List<ShowtimeCandidate> candidates) {
        long totalStarted = System.nanoTime();
        requireWellFormedCandidates(candidates);

        long rankingStarted = System.nanoTime();
        rankAndClearSelection(candidates);
        long rankingNanos = System.nanoTime() - rankingStarted;

        long partitioningStarted = System.nanoTime();
        PartitioningResult partitioning = buildSafeOptimizationComponents(candidates);
        long partitioningNanos = System.nanoTime() - partitioningStarted;

        long wisSortNanos = 0L;
        long predecessorNanos = 0L;
        long dpNanos = 0L;
        long reconstructionNanos = 0L;
        int largestComponentSize = 0;

        for (List<ShowtimeCandidate> component : partitioning.components()) {
            largestComponentSize = Math.max(largestComponentSize, component.size());

            long phaseStarted = System.nanoTime();
            component.sort(WIS_CANDIDATE_COMPARATOR);
            wisSortNanos += System.nanoTime() - phaseStarted;

            phaseStarted = System.nanoTime();
            int[] predecessors = computePredecessors(component);
            predecessorNanos += System.nanoTime() - phaseStarted;

            phaseStarted = System.nanoTime();
            DynamicProgram dynamicProgram = computeOptimalValues(component, predecessors);
            dpNanos += System.nanoTime() - phaseStarted;

            phaseStarted = System.nanoTime();
            reconstruct(component, predecessors, dynamicProgram.take());
            reconstructionNanos += System.nanoTime() - phaseStarted;
        }

        long invariantStarted = System.nanoTime();
        validateGlobalSelectionInvariant(candidates);
        long invariantNanos = System.nanoTime() - invariantStarted;

        int selectedCount = Math.toIntExact(candidates.stream().filter(ShowtimeCandidate::isSelected).count());
        return new SelectionDiagnostics(
                candidates.size(), partitioning.logicalPartitionCount(), partitioning.components().size(),
                largestComponentSize, rankingNanos, partitioningNanos, wisSortNanos,
                predecessorNanos, dpNanos, reconstructionNanos, invariantNanos,
                System.nanoTime() - totalStarted, selectedCount);
    }

    private void rankAndClearSelection(List<ShowtimeCandidate> candidates) {
        candidates.sort(GLOBAL_RANKING_COMPARATOR);
        int rank = 1;
        for (ShowtimeCandidate candidate : candidates) {
            candidate.setRankingPosition(rank++);
            candidate.setSelected(false);
        }
    }

    private void optimizeComponent(List<ShowtimeCandidate> component) {
        component.sort(WIS_CANDIDATE_COMPARATOR);
        int[] predecessors = computePredecessors(component);
        DynamicProgram dynamicProgram = computeOptimalValues(component, predecessors);
        reconstruct(component, predecessors, dynamicProgram.take());
    }

    private void requireWellFormedCandidates(List<ShowtimeCandidate> candidates) {
        if (candidates == null) {
            throw new IllegalStateException("Candidate list must not be null");
        }
        for (ShowtimeCandidate candidate : candidates) {
            if (candidate == null) {
                throw new IllegalStateException("Candidate must not be null");
            }
            if (candidate.getValidationStatus() == null) {
                throw new IllegalStateException("Candidate validation status must not be null");
            }
            if (candidate.getScore() == null) {
                throw new IllegalStateException("Candidate score must not be null");
            }
            if (candidate.getStartTime() == null || candidate.getEndTime() == null
                    || candidate.getOccupancyEndTime() == null) {
                throw new IllegalStateException("Candidate interval must be complete");
            }
            if (!candidate.getEndTime().isAfter(candidate.getStartTime())
                    || candidate.getOccupancyEndTime().isBefore(candidate.getEndTime())) {
                throw new IllegalStateException("Candidate interval is invalid");
            }
            if (candidate.getAuditoriumId() == null || candidate.getAuditoriumPublicId() == null
                    || candidate.getMovieVersionPublicId() == null) {
                throw new IllegalStateException("Candidate business identity must be complete");
            }
            if (candidate.getValidationStatus() == PreviewItemValidationStatus.VALID
                    && (candidate.getOperatingWindow() == null
                    || candidate.getOperatingWindow().getServiceDate() == null)) {
                throw new IllegalStateException("Valid candidate must retain its authoritative service date");
            }
        }
    }

    private PartitioningResult buildSafeOptimizationComponents(List<ShowtimeCandidate> candidates) {
        Map<LogicalPartitionKey, List<ShowtimeCandidate>> grouped = new HashMap<>();
        for (ShowtimeCandidate candidate : candidates) {
            if (candidate.getValidationStatus() != PreviewItemValidationStatus.VALID) {
                continue;
            }
            LocalDate serviceDate = candidate.getOperatingWindow().getServiceDate();
            LogicalPartitionKey key = new LogicalPartitionKey(
                    candidate.getAuditoriumId(), candidate.getAuditoriumPublicId(), serviceDate);
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
        }

        Map<AuditoriumKey, List<LogicalPartition>> byAuditorium = new HashMap<>();
        for (Map.Entry<LogicalPartitionKey, List<ShowtimeCandidate>> entry : grouped.entrySet()) {
            LogicalPartitionKey key = entry.getKey();
            List<ShowtimeCandidate> partitionCandidates = entry.getValue();
            Instant minimumStart = partitionCandidates.stream()
                    .map(ShowtimeCandidate::getStartTime).min(Comparator.naturalOrder()).orElseThrow();
            Instant maximumOccupancyEnd = partitionCandidates.stream()
                    .map(ShowtimeCandidate::getOccupancyEndTime).max(Comparator.naturalOrder()).orElseThrow();
            LogicalPartition partition = new LogicalPartition(
                    key.serviceDate(), minimumStart, maximumOccupancyEnd, partitionCandidates);
            byAuditorium.computeIfAbsent(
                    new AuditoriumKey(key.auditoriumId(), key.auditoriumPublicId()), ignored -> new ArrayList<>())
                    .add(partition);
        }

        List<Map.Entry<AuditoriumKey, List<LogicalPartition>>> auditoriums = new ArrayList<>(byAuditorium.entrySet());
        auditoriums.sort(Map.Entry.comparingByKey(Comparator
                .comparing(AuditoriumKey::auditoriumPublicId)
                .thenComparing(AuditoriumKey::auditoriumId)));

        List<List<ShowtimeCandidate>> components = new ArrayList<>();
        for (Map.Entry<AuditoriumKey, List<LogicalPartition>> auditoriumEntry : auditoriums) {
            List<LogicalPartition> partitions = auditoriumEntry.getValue();
            partitions.sort(LOGICAL_PARTITION_COMPARATOR);

            List<ShowtimeCandidate> currentCandidates = null;
            Instant currentMaximumOccupancyEnd = null;
            for (LogicalPartition partition : partitions) {
                if (currentCandidates == null
                        || !partition.minimumStart().isBefore(currentMaximumOccupancyEnd)) {
                    if (currentCandidates != null) {
                        components.add(currentCandidates);
                    }
                    currentCandidates = new ArrayList<>(partition.candidates());
                    currentMaximumOccupancyEnd = partition.maximumOccupancyEnd();
                } else {
                    currentCandidates.addAll(partition.candidates());
                    if (partition.maximumOccupancyEnd().isAfter(currentMaximumOccupancyEnd)) {
                        currentMaximumOccupancyEnd = partition.maximumOccupancyEnd();
                    }
                }
            }
            if (currentCandidates != null) {
                components.add(currentCandidates);
            }
        }
        return new PartitioningResult(grouped.size(), components);
    }

    private int[] computePredecessors(List<ShowtimeCandidate> candidates) {
        int[] predecessors = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Instant start = candidates.get(i).getStartTime();
            int low = 0;
            int high = i - 1;
            int predecessor = -1;
            while (low <= high) {
                int middle = (low + high) >>> 1;
                if (!candidates.get(middle).getOccupancyEndTime().isAfter(start)) {
                    predecessor = middle;
                    low = middle + 1;
                } else {
                    high = middle - 1;
                }
            }
            predecessors[i] = predecessor;
        }
        return predecessors;
    }

    private DynamicProgram computeOptimalValues(List<ShowtimeCandidate> candidates, int[] predecessors) {
        BigDecimal[] bestScore = new BigDecimal[candidates.size() + 1];
        boolean[] take = new boolean[candidates.size()];
        bestScore[0] = ZERO_SCORE;
        for (int i = 0; i < candidates.size(); i++) {
            BigDecimal include = candidates.get(i).getScore().add(bestScore[predecessors[i] + 1]);
            BigDecimal exclude = bestScore[i];
            if (include.compareTo(exclude) > 0) {
                bestScore[i + 1] = include;
                take[i] = true;
            } else {
                bestScore[i + 1] = exclude;
                take[i] = false;
            }
        }
        return new DynamicProgram(bestScore, take);
    }

    private void reconstruct(List<ShowtimeCandidate> candidates, int[] predecessors, boolean[] take) {
        int index = candidates.size() - 1;
        while (index >= 0) {
            if (take[index]) {
                candidates.get(index).setSelected(true);
                index = predecessors[index];
            } else {
                index--;
            }
        }
    }

    void validateGlobalSelectionInvariant(List<ShowtimeCandidate> candidates) {
        Map<AuditoriumKey, List<ShowtimeCandidate>> selectedByAuditorium = new HashMap<>();
        for (ShowtimeCandidate candidate : candidates) {
            if (candidate.isSelected()) {
                AuditoriumKey key = new AuditoriumKey(
                        candidate.getAuditoriumId(), candidate.getAuditoriumPublicId());
                selectedByAuditorium.computeIfAbsent(key, ignored -> new ArrayList<>()).add(candidate);
            }
        }

        for (Map.Entry<AuditoriumKey, List<ShowtimeCandidate>> entry : selectedByAuditorium.entrySet()) {
            List<ShowtimeCandidate> selected = entry.getValue();
            selected.sort(Comparator.comparing(ShowtimeCandidate::getStartTime)
                    .thenComparing(ShowtimeCandidate::getOccupancyEndTime)
                    .thenComparing(ShowtimeCandidate::getMovieVersionPublicId));
            Instant maximumOccupancyEnd = null;
            for (ShowtimeCandidate candidate : selected) {
                if (maximumOccupancyEnd != null && candidate.getStartTime().isBefore(maximumOccupancyEnd)) {
                    throw new BusinessException(
                            ErrorCode.AUTO_SCHEDULE_SELECTION_INVARIANT_VIOLATION,
                            "Optimized candidates overlap in auditorium " + entry.getKey().auditoriumPublicId());
                }
                if (maximumOccupancyEnd == null
                        || candidate.getOccupancyEndTime().isAfter(maximumOccupancyEnd)) {
                    maximumOccupancyEnd = candidate.getOccupancyEndTime();
                }
            }
        }
    }

    record SelectionDiagnostics(int candidateCount,
                                int logicalPartitionCount,
                                int optimizationComponentCount,
                                int largestComponentSize,
                                long rankingNanos,
                                long partitioningNanos,
                                long wisSortNanos,
                                long predecessorNanos,
                                long dpNanos,
                                long reconstructionNanos,
                                long invariantNanos,
                                long totalNanos,
                                int selectedCount) {
    }

    private record LogicalPartitionKey(Long auditoriumId,
                                       String auditoriumPublicId,
                                       LocalDate serviceDate) {
    }

    private record AuditoriumKey(Long auditoriumId, String auditoriumPublicId) {
    }

    private record LogicalPartition(LocalDate serviceDate,
                                    Instant minimumStart,
                                    Instant maximumOccupancyEnd,
                                    List<ShowtimeCandidate> candidates) {
    }

    private record PartitioningResult(int logicalPartitionCount,
                                      List<List<ShowtimeCandidate>> components) {
    }

    private record DynamicProgram(BigDecimal[] bestScore, boolean[] take) {
    }
}
