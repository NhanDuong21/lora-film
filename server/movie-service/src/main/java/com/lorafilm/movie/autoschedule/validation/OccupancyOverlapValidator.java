package com.lorafilm.movie.autoschedule.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Canonical pure backend validator for half-open occupancy intervals.
 *
 * <p>Intervals are partitioned only by auditorium. Exact adjacency is valid. The sweep retains
 * the maximum prior occupancy end so nested intervals cannot hide a later conflict.</p>
 */
public final class OccupancyOverlapValidator {

    private static final Comparator<OccupancyInterval> INTERVAL_ORDER = Comparator
            .comparing(OccupancyInterval::startTime)
            .thenComparing(OccupancyInterval::occupancyEndTime)
            .thenComparing(OccupancyInterval::itemPublicId);

    private OccupancyOverlapValidator() {
    }

    public static Optional<OccupancyOverlapConflict> findConflict(
            Collection<OccupancyInterval> intervals) {
        Objects.requireNonNull(intervals, "intervals");

        Map<Long, List<OccupancyInterval>> byAuditorium = new TreeMap<>();
        for (OccupancyInterval interval : new ArrayList<>(intervals)) {
            validateCanonicalInput(interval);
            byAuditorium.computeIfAbsent(interval.auditoriumId(), ignored -> new ArrayList<>())
                    .add(interval);
        }

        for (Map.Entry<Long, List<OccupancyInterval>> entry : byAuditorium.entrySet()) {
            List<OccupancyInterval> partition = new ArrayList<>(entry.getValue());
            partition.sort(INTERVAL_ORDER);

            OccupancyInterval maximumEndInterval = null;
            for (OccupancyInterval current : partition) {
                if (maximumEndInterval != null
                        && current.startTime().isBefore(maximumEndInterval.occupancyEndTime())) {
                    return Optional.of(new OccupancyOverlapConflict(
                            entry.getKey(),
                            maximumEndInterval.itemPublicId(),
                            current.itemPublicId()));
                }
                if (maximumEndInterval == null
                        || current.occupancyEndTime().isAfter(maximumEndInterval.occupancyEndTime())) {
                    maximumEndInterval = current;
                }
            }
        }

        return Optional.empty();
    }

    private static void validateCanonicalInput(OccupancyInterval interval) {
        Objects.requireNonNull(interval, "interval");
        Objects.requireNonNull(interval.auditoriumId(), "auditoriumId");
        Objects.requireNonNull(interval.startTime(), "startTime");
        Objects.requireNonNull(interval.occupancyEndTime(), "occupancyEndTime");
        Objects.requireNonNull(interval.itemPublicId(), "itemPublicId");
        if (!interval.startTime().isBefore(interval.occupancyEndTime())) {
            throw new IllegalArgumentException("Occupancy interval must have positive duration");
        }
    }
}
