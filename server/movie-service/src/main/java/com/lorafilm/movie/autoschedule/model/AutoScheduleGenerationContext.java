package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

public final class AutoScheduleGenerationContext {

    private final CinemaSnapshot cinema;
    private final LocalDate scheduleFrom;
    private final LocalDate scheduleTo;
    private final int slotGranularityMinutes;
    private final int candidateLimit;
    private final AutoScheduleStrategy strategy;
    private final String strategyVersion;
    private final List<AuditoriumSnapshot> auditoriums;
    private final List<MovieVersionSnapshot> movieVersions;
    private final List<OperatingWindow> operatingWindows;
    private final Set<Integer> configuredOperatingDays;
    private final ImmutableIntervalIndex cinemaClosures;
    private final Map<Long, ImmutableIntervalIndex> maintenanceByAuditorium;
    private final Map<Long, ImmutableIntervalIndex> showtimeConflictsByAuditorium;
    private final Map<Long, ContinuityIndex> continuityByAuditorium;
    private final Map<MovieServiceDateKey, Integer> existingShowtimeCounts;
    private final Instant planningStart;
    private final Instant planningEnd;

    public AutoScheduleGenerationContext(CinemaSnapshot cinema,
                                         LocalDate scheduleFrom,
                                         LocalDate scheduleTo,
                                         int slotGranularityMinutes,
                                         int candidateLimit,
                                         AutoScheduleStrategy strategy,
                                         String strategyVersion,
                                         List<AuditoriumSnapshot> auditoriums,
                                         List<MovieVersionSnapshot> movieVersions,
                                         List<OperatingWindow> operatingWindows,
                                         Set<Integer> configuredOperatingDays,
                                         ImmutableIntervalIndex cinemaClosures,
                                         Map<Long, ImmutableIntervalIndex> maintenanceByAuditorium,
                                         Map<Long, ImmutableIntervalIndex> showtimeConflictsByAuditorium,
                                         Map<Long, ContinuityIndex> continuityByAuditorium,
                                         Instant planningStart,
                                         Instant planningEnd) {
        this(cinema, scheduleFrom, scheduleTo, slotGranularityMinutes, candidateLimit,
                strategy, strategyVersion, auditoriums, movieVersions, operatingWindows,
                configuredOperatingDays, cinemaClosures, maintenanceByAuditorium,
                showtimeConflictsByAuditorium, continuityByAuditorium, Map.of(),
                planningStart, planningEnd);
    }

    public AutoScheduleGenerationContext(CinemaSnapshot cinema,
                                         LocalDate scheduleFrom,
                                         LocalDate scheduleTo,
                                         int slotGranularityMinutes,
                                         int candidateLimit,
                                         AutoScheduleStrategy strategy,
                                         String strategyVersion,
                                         List<AuditoriumSnapshot> auditoriums,
                                         List<MovieVersionSnapshot> movieVersions,
                                         List<OperatingWindow> operatingWindows,
                                         Set<Integer> configuredOperatingDays,
                                         ImmutableIntervalIndex cinemaClosures,
                                         Map<Long, ImmutableIntervalIndex> maintenanceByAuditorium,
                                         Map<Long, ImmutableIntervalIndex> showtimeConflictsByAuditorium,
                                         Map<Long, ContinuityIndex> continuityByAuditorium,
                                         Map<MovieServiceDateKey, Integer> existingShowtimeCounts,
                                         Instant planningStart,
                                         Instant planningEnd) {
        this.cinema = cinema;
        this.scheduleFrom = scheduleFrom;
        this.scheduleTo = scheduleTo;
        this.slotGranularityMinutes = slotGranularityMinutes;
        this.candidateLimit = candidateLimit;
        this.strategy = strategy;
        this.strategyVersion = strategyVersion;
        this.auditoriums = List.copyOf(auditoriums);
        this.movieVersions = List.copyOf(movieVersions);
        this.operatingWindows = List.copyOf(operatingWindows);
        this.configuredOperatingDays = Set.copyOf(configuredOperatingDays);
        this.cinemaClosures = cinemaClosures;
        this.maintenanceByAuditorium = Map.copyOf(maintenanceByAuditorium);
        this.showtimeConflictsByAuditorium = Map.copyOf(showtimeConflictsByAuditorium);
        this.continuityByAuditorium = Map.copyOf(continuityByAuditorium);
        this.existingShowtimeCounts = Collections.unmodifiableMap(
                new LinkedHashMap<>(existingShowtimeCounts));
        this.planningStart = planningStart;
        this.planningEnd = planningEnd;
    }

    public CinemaSnapshot getCinema() { return cinema; }
    public LocalDate getScheduleFrom() { return scheduleFrom; }
    public LocalDate getScheduleTo() { return scheduleTo; }
    public int getSlotGranularityMinutes() { return slotGranularityMinutes; }
    public int getCandidateLimit() { return candidateLimit; }
    public AutoScheduleStrategy getStrategy() { return strategy; }
    public String getStrategyVersion() { return strategyVersion; }
    public List<AuditoriumSnapshot> getAuditoriums() { return auditoriums; }
    public List<MovieVersionSnapshot> getMovieVersions() { return movieVersions; }
    public List<OperatingWindow> getOperatingWindows() { return operatingWindows; }
    public Set<Integer> getConfiguredOperatingDays() { return configuredOperatingDays; }
    public ImmutableIntervalIndex getCinemaClosures() { return cinemaClosures; }
    public ImmutableIntervalIndex maintenanceFor(Long auditoriumId) {
        return maintenanceByAuditorium.getOrDefault(auditoriumId, ImmutableIntervalIndex.empty());
    }
    public ImmutableIntervalIndex showtimeConflictsFor(Long auditoriumId) {
        return showtimeConflictsByAuditorium.getOrDefault(auditoriumId, ImmutableIntervalIndex.empty());
    }
    public ContinuityIndex continuityFor(Long auditoriumId) {
        return continuityByAuditorium.getOrDefault(auditoriumId, ContinuityIndex.empty());
    }
    public Map<MovieServiceDateKey, Integer> getExistingShowtimeCounts() {
        return existingShowtimeCounts;
    }
    public int existingShowtimeCount(LocalDate serviceDate, Long movieId) {
        return existingShowtimeCounts.getOrDefault(new MovieServiceDateKey(serviceDate, movieId), 0);
    }
    public Instant getPlanningStart() { return planningStart; }
    public Instant getPlanningEnd() { return planningEnd; }

    public record CinemaSnapshot(Long id,
                                 String publicId,
                                 String name,
                                 ZoneId zoneId,
                                 CinemaStatus status,
                                 boolean deleted) {
    }

    public record AuditoriumSnapshot(Long id,
                                     String publicId,
                                     Long cinemaId,
                                     String name,
                                     Integer capacity,
                                     Integer cleaningBufferMinutes,
                                     AuditoriumStatus status,
                                     boolean deleted,
                                     ScreenType screenType) {
        public AuditoriumSnapshot(Long id, String publicId, Long cinemaId, String name,
                                  Integer capacity, Integer cleaningBufferMinutes,
                                  AuditoriumStatus status, boolean deleted) {
            this(id, publicId, cinemaId, name, capacity, cleaningBufferMinutes,
                    status, deleted, null);
        }

        public int effectiveCleaningBufferMinutes() {
            return cleaningBufferMinutes == null ? 0 : cleaningBufferMinutes;
        }
    }

    public record MovieSnapshot(Long id,
                                String publicId,
                                String title,
                                Integer durationMinutes,
                                LocalDate releaseDate,
                                LocalDate endDate,
                                MovieStatus status,
                                boolean deleted) {
    }

    public record MovieVersionSnapshot(Long id,
                                       String publicId,
                                       Long movieId,
                                       ActiveStatus status,
                                       boolean deleted,
                                       MovieSnapshot movie,
                                       MovieFormat format) {
        public MovieVersionSnapshot(Long id, String publicId, Long movieId,
                                    ActiveStatus status, boolean deleted, MovieSnapshot movie) {
            this(id, publicId, movieId, status, deleted, movie, null);
        }
    }

    public record MovieServiceDateKey(LocalDate serviceDate, Long movieId) {
    }
}
