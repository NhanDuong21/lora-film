package com.lorafilm.movie.autoschedule.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Individual occupancy-end facts kept separately from merged conflict intervals. */
public final class ContinuityIndex {

    private static final ContinuityIndex EMPTY = new ContinuityIndex(List.of());

    private final List<Instant> occupancyEnds;

    private ContinuityIndex(List<Instant> occupancyEnds) {
        this.occupancyEnds = occupancyEnds;
    }

    public static ContinuityIndex empty() {
        return EMPTY;
    }

    public static ContinuityIndex of(List<Instant> occupancyEnds) {
        if (occupancyEnds == null || occupancyEnds.isEmpty()) {
            return EMPTY;
        }
        return new ContinuityIndex(occupancyEnds.stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.naturalOrder())
                .toList());
    }

    public Optional<Instant> latestAtOrBefore(Instant instant) {
        int low = 0;
        int high = occupancyEnds.size() - 1;
        int result = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Instant value = occupancyEnds.get(mid);
            if (!value.isAfter(instant)) {
                result = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return result < 0 ? Optional.empty() : Optional.of(occupancyEnds.get(result));
    }

    public List<Instant> occupancyEnds() {
        return occupancyEnds;
    }
}
