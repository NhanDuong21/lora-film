package com.lorafilm.movie.autoschedule.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Immutable half-open interval index backed by sorted, merged ranges. */
public final class ImmutableIntervalIndex {

    private static final ImmutableIntervalIndex EMPTY = new ImmutableIntervalIndex(List.of());

    private final List<Interval> intervals;

    private ImmutableIntervalIndex(List<Interval> intervals) {
        this.intervals = intervals;
    }

    public static ImmutableIntervalIndex empty() {
        return EMPTY;
    }

    public static ImmutableIntervalIndex of(List<Interval> source) {
        if (source == null || source.isEmpty()) {
            return EMPTY;
        }

        List<Interval> sorted = source.stream()
                .filter(interval -> interval != null
                        && interval.start() != null
                        && interval.end() != null
                        && interval.end().isAfter(interval.start()))
                .sorted(Comparator.comparing(Interval::start).thenComparing(Interval::end))
                .toList();
        if (sorted.isEmpty()) {
            return EMPTY;
        }

        List<Interval> merged = new ArrayList<>();
        Instant currentStart = sorted.get(0).start();
        Instant currentEnd = sorted.get(0).end();
        for (int i = 1; i < sorted.size(); i++) {
            Interval next = sorted.get(i);
            if (!next.start().isAfter(currentEnd)) {
                if (next.end().isAfter(currentEnd)) {
                    currentEnd = next.end();
                }
            } else {
                merged.add(new Interval(currentStart, currentEnd));
                currentStart = next.start();
                currentEnd = next.end();
            }
        }
        merged.add(new Interval(currentStart, currentEnd));
        return new ImmutableIntervalIndex(List.copyOf(merged));
    }

    public boolean overlaps(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start) || intervals.isEmpty()) {
            return false;
        }

        int low = 0;
        int high = intervals.size() - 1;
        int candidateIndex = -1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (intervals.get(mid).start().isBefore(end)) {
                candidateIndex = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return candidateIndex >= 0 && intervals.get(candidateIndex).end().isAfter(start);
    }

    public List<Interval> intervals() {
        return intervals;
    }

    public record Interval(Instant start, Instant end) {
    }
}
