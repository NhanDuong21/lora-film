package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.CandidateSlot;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.service.UniqueCandidateSlotTraversal;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class UniqueCandidateSlotTraversalImpl implements UniqueCandidateSlotTraversal {

    @Override
    public long traverse(AutoScheduleGenerationContext context,
                         long stopAfter,
                         Consumer<CandidateSlot> consumer) {
        if (context.getSlotGranularityMinutes() <= 0) {
            throw new IllegalArgumentException("Slot granularity must be positive");
        }

        List<OperatingWindow> windows = context.getOperatingWindows().stream()
                .sorted(Comparator
                        .comparing(OperatingWindow::getServiceDate,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OperatingWindow::getOpenInstant)
                        .thenComparing(OperatingWindow::getCloseInstant))
                .toList();
        List<AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriums = context.getAuditoriums().stream()
                .sorted(Comparator.comparing(AutoScheduleGenerationContext.AuditoriumSnapshot::publicId))
                .toList();
        List<AutoScheduleGenerationContext.MovieVersionSnapshot> versions = context.getMovieVersions().stream()
                .sorted(Comparator.comparing(AutoScheduleGenerationContext.MovieVersionSnapshot::publicId))
                .toList();

        Set<CandidateKey> emitted = new HashSet<>();
        long count = 0;
        long stepMinutes = context.getSlotGranularityMinutes();

        for (OperatingWindow window : windows) {
            for (AutoScheduleGenerationContext.AuditoriumSnapshot auditorium : auditoriums) {
                for (AutoScheduleGenerationContext.MovieVersionSnapshot version : versions) {
                    Integer duration = version.movie().durationMinutes();
                    if (duration == null || duration <= 0) {
                        continue;
                    }

                    Instant lastStart;
                    try {
                        lastStart = window.getCloseInstant().minus(duration.longValue(), ChronoUnit.MINUTES);
                    } catch (RuntimeException overflow) {
                        continue;
                    }
                    if (lastStart.isBefore(window.getOpenInstant())) {
                        continue;
                    }

                    long positions = Math.addExact(
                            Math.floorDiv(Duration.between(window.getOpenInstant(), lastStart).toMinutes(),
                                    stepMinutes),
                            1L);
                    for (long position = 0; position < positions; position++) {
                        long offsetMinutes = Math.multiplyExact(position, stepMinutes);
                        Instant start = window.getOpenInstant().plus(offsetMinutes, ChronoUnit.MINUTES);
                        CandidateKey key = new CandidateKey(auditorium.id(), version.id(), start);
                        if (emitted.add(key)) {
                            count = Math.addExact(count, 1L);
                            if (count > stopAfter) {
                                return count;
                            }
                            if (consumer != null) {
                                Instant end = start.plus(duration.longValue(), ChronoUnit.MINUTES);
                                Instant occupancyEnd = end.plus(
                                        auditorium.effectiveCleaningBufferMinutes(), ChronoUnit.MINUTES);
                                consumer.accept(new CandidateSlot(
                                        window.getServiceDate(), window, auditorium, version,
                                        start, end, occupancyEnd));
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    private record CandidateKey(Long auditoriumId, Long movieVersionId, Instant startTime) {
    }
}
