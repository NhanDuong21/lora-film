package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.AutoScheduleStrategyVersions;
import com.lorafilm.movie.autoschedule.model.ContinuityIndex;
import com.lorafilm.movie.autoschedule.model.ImmutableIntervalIndex;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.service.AutoScheduleGenerationContextLoader;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AutoScheduleGenerationContextLoaderImpl implements AutoScheduleGenerationContextLoader {

    public static final int MAX_CANDIDATES = 10_000;

    private final CinemaOperatingHourRepository operatingHourRepository;
    private final CinemaClosurePeriodRepository closureRepository;
    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaOperatingWindowResolver windowResolver;

    public AutoScheduleGenerationContextLoaderImpl(CinemaOperatingHourRepository operatingHourRepository,
                                                   CinemaClosurePeriodRepository closureRepository,
                                                   AuditoriumMaintenanceWindowRepository maintenanceRepository,
                                                   ShowtimeRepository showtimeRepository,
                                                   CinemaOperatingWindowResolver windowResolver) {
        this.operatingHourRepository = operatingHourRepository;
        this.closureRepository = closureRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.showtimeRepository = showtimeRepository;
        this.windowResolver = windowResolver;
    }

    @Override
    public AutoScheduleGenerationContext load(NormalizedGeneratePreviewRequest request,
                                              Cinema cinema,
                                              List<Auditorium> auditoriums,
                                              List<MovieVersion> movieVersions) {
        ZoneId zoneId = ZoneId.of(cinema.getTimezone());
        AutoScheduleGenerationContext.CinemaSnapshot cinemaSnapshot =
                new AutoScheduleGenerationContext.CinemaSnapshot(
                        cinema.getId(), cinema.getPublicId(), cinema.getName(), zoneId,
                        cinema.getStatus(), cinema.getDeletedAt() != null);

        List<AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriumSnapshots = auditoriums.stream()
                .map(auditorium -> new AutoScheduleGenerationContext.AuditoriumSnapshot(
                        auditorium.getId(), auditorium.getPublicId(), auditorium.getCinema().getId(),
                        auditorium.getName(), auditorium.getCapacity(), auditorium.getCleaningBufferMinutes(),
                        auditorium.getStatus(), auditorium.getDeletedAt() != null))
                .sorted(Comparator.comparing(AutoScheduleGenerationContext.AuditoriumSnapshot::publicId))
                .toList();

        List<AutoScheduleGenerationContext.MovieVersionSnapshot> versionSnapshots = movieVersions.stream()
                .map(this::snapshotVersion)
                .sorted(Comparator.comparing(AutoScheduleGenerationContext.MovieVersionSnapshot::publicId))
                .toList();

        List<CinemaOperatingHour> operatingHours = operatingHourRepository.findByCinemaId(cinema.getId());
        List<OperatingWindow> windows = windowResolver.resolve(
                cinema, request.getScheduleFrom(), request.getScheduleTo(), operatingHours);
        Set<Integer> configuredDays = new HashSet<>();
        for (CinemaOperatingHour hour : operatingHours) {
            if (hour.getDayOfWeek() != null) {
                configuredDays.add(hour.getDayOfWeek());
            }
        }

        if (windows.isEmpty()) {
            return new AutoScheduleGenerationContext(
                    cinemaSnapshot, request.getScheduleFrom(), request.getScheduleTo(),
                    request.getSlotGranularityMinutes(), MAX_CANDIDATES,
                    AutoScheduleStrategy.BALANCED, AutoScheduleStrategyVersions.CURRENT,
                    auditoriumSnapshots, versionSnapshots, windows, configuredDays,
                    ImmutableIntervalIndex.empty(), Map.of(), Map.of(), Map.of(), null, null);
        }

        Instant planningStart = windows.stream().map(OperatingWindow::getOpenInstant)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant latestClose = windows.stream().map(OperatingWindow::getCloseInstant)
                .max(Comparator.naturalOrder()).orElseThrow();
        int maxBuffer = auditoriumSnapshots.stream()
                .map(AutoScheduleGenerationContext.AuditoriumSnapshot::effectiveCleaningBufferMinutes)
                .filter(buffer -> buffer > 0)
                .max(Integer::compareTo).orElse(0);
        Instant planningEnd = latestClose.plus(maxBuffer, ChronoUnit.MINUTES);
        Instant showtimeLowerBound = planningStart.minus((long) maxBuffer + 31L, ChronoUnit.MINUTES);

        List<CinemaClosurePeriod> closures = closureRepository.findOverlappingClosures(
                cinema.getId(), planningStart, planningEnd);
        ImmutableIntervalIndex closureIndex = ImmutableIntervalIndex.of(closures.stream()
                .map(closure -> new ImmutableIntervalIndex.Interval(closure.getStartTime(), closure.getEndTime()))
                .toList());

        List<Long> auditoriumIds = auditoriumSnapshots.stream()
                .map(AutoScheduleGenerationContext.AuditoriumSnapshot::id).toList();
        List<AuditoriumMaintenanceWindow> maintenance = maintenanceRepository
                .findActiveOverlapsForAutoSchedule(
                        auditoriumIds, ActionStatus.ACTIVE, planningStart, planningEnd);
        Map<Long, ImmutableIntervalIndex> maintenanceIndexes = buildMaintenanceIndexes(maintenance);

        List<Showtime> existingShowtimes = showtimeRepository.findBlockingFactsForAutoSchedule(
                auditoriumIds, showtimeLowerBound, planningEnd);
        ExistingShowtimeIndexes showtimeIndexes = buildShowtimeIndexes(existingShowtimes, auditoriumSnapshots);

        return new AutoScheduleGenerationContext(
                cinemaSnapshot, request.getScheduleFrom(), request.getScheduleTo(),
                request.getSlotGranularityMinutes(), MAX_CANDIDATES,
                AutoScheduleStrategy.BALANCED, AutoScheduleStrategyVersions.CURRENT,
                auditoriumSnapshots, versionSnapshots, windows, configuredDays,
                closureIndex, maintenanceIndexes, showtimeIndexes.conflicts(),
                showtimeIndexes.continuity(), planningStart, planningEnd);
    }

    private AutoScheduleGenerationContext.MovieVersionSnapshot snapshotVersion(MovieVersion version) {
        Movie movie = version.getMovie();
        AutoScheduleGenerationContext.MovieSnapshot movieSnapshot =
                new AutoScheduleGenerationContext.MovieSnapshot(
                        movie.getId(), movie.getPublicId(), movie.getTitle(), movie.getDurationMinutes(),
                        movie.getReleaseDate(), movie.getEndDate(), movie.getStatus(), movie.getDeletedAt() != null);
        return new AutoScheduleGenerationContext.MovieVersionSnapshot(
                version.getId(), version.getPublicId(), movie.getId(), version.getStatus(),
                version.getDeletedAt() != null, movieSnapshot);
    }

    private Map<Long, ImmutableIntervalIndex> buildMaintenanceIndexes(
            List<AuditoriumMaintenanceWindow> maintenance) {
        Map<Long, List<ImmutableIntervalIndex.Interval>> grouped = new HashMap<>();
        for (AuditoriumMaintenanceWindow window : maintenance) {
            grouped.computeIfAbsent(window.getAuditorium().getId(), ignored -> new ArrayList<>())
                    .add(new ImmutableIntervalIndex.Interval(window.getStartTime(), window.getEndTime()));
        }
        Map<Long, ImmutableIntervalIndex> indexes = new HashMap<>();
        grouped.forEach((id, intervals) -> indexes.put(id, ImmutableIntervalIndex.of(intervals)));
        return indexes;
    }

    private ExistingShowtimeIndexes buildShowtimeIndexes(
            List<Showtime> showtimes,
            List<AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriums) {
        Map<Long, AutoScheduleGenerationContext.AuditoriumSnapshot> auditoriumById = new HashMap<>();
        auditoriums.forEach(auditorium -> auditoriumById.put(auditorium.id(), auditorium));

        Map<Long, List<ImmutableIntervalIndex.Interval>> conflictFacts = new HashMap<>();
        Map<Long, List<Instant>> continuityFacts = new HashMap<>();
        for (Showtime showtime : showtimes) {
            Long auditoriumId = showtime.getAuditorium().getId();
            AutoScheduleGenerationContext.AuditoriumSnapshot auditorium = auditoriumById.get(auditoriumId);
            if (auditorium == null) {
                continue;
            }
            Instant occupancyEnd = showtime.getEndTime().plus(
                    Math.max(0, auditorium.effectiveCleaningBufferMinutes()), ChronoUnit.MINUTES);
            conflictFacts.computeIfAbsent(auditoriumId, ignored -> new ArrayList<>())
                    .add(new ImmutableIntervalIndex.Interval(showtime.getStartTime(), occupancyEnd));
            continuityFacts.computeIfAbsent(auditoriumId, ignored -> new ArrayList<>()).add(occupancyEnd);
        }

        Map<Long, ImmutableIntervalIndex> conflicts = new HashMap<>();
        conflictFacts.forEach((id, intervals) -> conflicts.put(id, ImmutableIntervalIndex.of(intervals)));
        Map<Long, ContinuityIndex> continuity = new HashMap<>();
        continuityFacts.forEach((id, ends) -> continuity.put(id, ContinuityIndex.of(ends)));
        return new ExistingShowtimeIndexes(conflicts, continuity);
    }

    private record ExistingShowtimeIndexes(Map<Long, ImmutableIntervalIndex> conflicts,
                                           Map<Long, ContinuityIndex> continuity) {
    }
}
