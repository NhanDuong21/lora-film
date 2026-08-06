package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class CandidateScoringContext {
    private final Cinema cinema;
    private final List<OperatingWindow> operatingWindows;
    private final List<Showtime> existingShowtimes;
    private final AutoScheduleGenerationContext generationContext;
    private AutoScheduleOptimizationResult optimizationResult;

    public CandidateScoringContext(Cinema cinema, List<OperatingWindow> operatingWindows, List<Showtime> existingShowtimes) {
        this.cinema = cinema;
        this.operatingWindows = operatingWindows;
        this.existingShowtimes = existingShowtimes;
        this.generationContext = null;
    }

    public CandidateScoringContext(AutoScheduleGenerationContext generationContext) {
        this.cinema = null;
        this.operatingWindows = generationContext.getOperatingWindows();
        this.existingShowtimes = List.of();
        this.generationContext = generationContext;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public List<OperatingWindow> getOperatingWindows() {
        return operatingWindows;
    }

    public List<Showtime> getExistingShowtimes() {
        return existingShowtimes;
    }

    public AutoScheduleGenerationContext getGenerationContext() {
        if (generationContext == null) {
            throw new IllegalStateException("Immutable generation context is required by this strategy");
        }
        return generationContext;
    }

    public ZoneId getZoneId() {
        return generationContext != null
                ? generationContext.getCinema().zoneId()
                : ZoneId.of(cinema.getTimezone());
    }

    public AutoScheduleOptimizationResult getOptimizationResult() {
        return optimizationResult;
    }

    public void setOptimizationResult(AutoScheduleOptimizationResult optimizationResult) {
        this.optimizationResult = optimizationResult;
    }

    public Optional<Instant> findClosestPreviousOccupancyEnd(Long auditoriumId, Instant candidateStart) {
        if (generationContext != null) {
            return generationContext.continuityFor(auditoriumId).latestAtOrBefore(candidateStart);
        }
        return existingShowtimes.stream()
                .filter(st -> st.getAuditorium().getId().equals(auditoriumId))
                .map(st -> st.getEndTime().plus(
                        st.getAuditorium().getCleaningBufferMinutes() == null
                                ? 0 : st.getAuditorium().getCleaningBufferMinutes(),
                        ChronoUnit.MINUTES))
                .filter(end -> !end.isAfter(candidateStart))
                .max(Instant::compareTo);
    }
}
