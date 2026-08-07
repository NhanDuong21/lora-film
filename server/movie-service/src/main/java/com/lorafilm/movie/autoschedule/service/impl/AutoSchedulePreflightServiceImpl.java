package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.auditorium.repository.AuditoriumMaintenanceWindowRepository;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreflightRequest;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.autoschedule.model.AutoSchedulePreflightResult;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreflightService;
import com.lorafilm.movie.autoschedule.service.CinemaOperatingWindowResolver;
import com.lorafilm.movie.autoschedule.service.MovieFormatCompatibilityPolicy;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaClosurePeriodRepository;
import com.lorafilm.movie.cinema.repository.CinemaOperatingHourRepository;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import com.lorafilm.movie.movie.repository.MovieVersionRepository;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AutoSchedulePreflightServiceImpl implements AutoSchedulePreflightService {

    private static final int SLOT_MINUTES = 15;
    private static final Set<Integer> PLANNING_PRESETS = Set.of(1, 3, 7);

    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final MovieVersionRepository movieVersionRepository;
    private final CinemaOperatingHourRepository operatingHourRepository;
    private final CinemaClosurePeriodRepository closureRepository;
    private final AuditoriumMaintenanceWindowRepository maintenanceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaOperatingWindowResolver operatingWindowResolver;
    private final MovieFormatCompatibilityPolicy compatibilityPolicy;
    private final PricePolicyResolver pricePolicyResolver;
    private final Clock clock;
    private AutoScheduleMetrics metrics = AutoScheduleMetrics.noop();

    public AutoSchedulePreflightServiceImpl(CinemaRepository cinemaRepository,
                                            AuditoriumRepository auditoriumRepository,
                                            MovieVersionRepository movieVersionRepository,
                                            CinemaOperatingHourRepository operatingHourRepository,
                                            CinemaClosurePeriodRepository closureRepository,
                                            AuditoriumMaintenanceWindowRepository maintenanceRepository,
                                            ShowtimeRepository showtimeRepository,
                                            CinemaOperatingWindowResolver operatingWindowResolver,
                                            MovieFormatCompatibilityPolicy compatibilityPolicy,
                                            PricePolicyResolver pricePolicyResolver,
                                            Clock clock) {
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.movieVersionRepository = movieVersionRepository;
        this.operatingHourRepository = operatingHourRepository;
        this.closureRepository = closureRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.showtimeRepository = showtimeRepository;
        this.operatingWindowResolver = operatingWindowResolver;
        this.compatibilityPolicy = compatibilityPolicy;
        this.pricePolicyResolver = pricePolicyResolver;
        this.clock = clock;
    }

    @Autowired
    void setMetrics(AutoScheduleMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    @Transactional(readOnly = true)
    public AutoSchedulePreflightResult prepare(AutoSchedulePreflightRequest request) {
        long started = System.nanoTime();
        int planningDays = request.getPlanningDays() == null ? 1 : request.getPlanningDays();
        if (!PLANNING_PRESETS.contains(planningDays)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_INVALID_DATE_RANGE,
                    "Planning days must be one of 1, 3, or 7");
        }

        String cinemaPublicId = trim(request.getCinemaPublicId());
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(cinema.getTimezone());
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE);
        }

        LocalDate planningFrom = Instant.now(clock).atZone(zoneId).toLocalDate().plusDays(1);
        LocalDate planningTo = planningFrom.plusDays(planningDays - 1L);
        List<AutoSchedulePreflightResponse.Blocker> blockers = new ArrayList<>();
        if (cinema.getStatus() != CinemaStatus.ACTIVE) {
            blockers.add(blocker("CINEMA_NOT_ACTIVE", "Rạp phải ở trạng thái đang hoạt động trước khi có thể tạo lịch",
                    "/admin/cinemas/" + cinemaPublicId));
        }

        List<MovieVersion> versions = filterVersions(
                movieVersionRepository.findEligibleForAutoSchedule(
                        ActiveStatus.ACTIVE,
                        List.of(MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING),
                        planningFrom,
                        planningTo),
                normalizedSet(request.getIncludeMovieVersionPublicIds()),
                normalizedSet(request.getExcludeMovieVersionPublicIds()));
        List<Auditorium> auditoriums = filterAuditoriums(
                auditoriumRepository.findByCinemaIdAndStatusAndDeletedAtIsNull(
                        cinema.getId(), AuditoriumStatus.ACTIVE),
                normalizedSet(request.getIncludeAuditoriumPublicIds()),
                normalizedSet(request.getExcludeAuditoriumPublicIds()));

        if (versions.isEmpty()) {
            blockers.add(blocker("NO_ELIGIBLE_VERSIONS",
                    "Không có phiên bản phim đang hoạt động nào phù hợp với khoảng ngày đã chọn",
                    "/admin/movies"));
        }
        if (auditoriums.isEmpty()) {
            blockers.add(blocker("NO_ELIGIBLE_AUDITORIUMS",
                    "Không có phòng chiếu đang hoạt động nào có sức chứa và thời gian vệ sinh hợp lệ",
                    "/admin/cinemas/" + cinemaPublicId + "/auditoriums"));
        }

        Set<AutoSchedulePreflightResult.CompatiblePair> compatiblePairs = compatiblePairs(versions, auditoriums);
        if (!versions.isEmpty() && !auditoriums.isEmpty() && compatiblePairs.isEmpty()) {
            blockers.add(blocker("NO_COMPATIBLE_PAIRS",
                    "Định dạng của các phim đủ điều kiện không tương thích với bất kỳ phòng chiếu đang hoạt động nào",
                    "/admin/cinemas/" + cinemaPublicId + "/auditoriums"));
        }

        List<CinemaOperatingHour> operatingHours = operatingHourRepository.findByCinemaId(cinema.getId());
        List<OperatingWindow> operatingWindows = operatingWindowResolver.resolve(
                cinema, planningFrom, planningTo, operatingHours);
        if (operatingWindows.size() < planningDays) {
            blockers.add(blocker("MISSING_OPERATING_HOURS",
                    "Mỗi ngày trong phạm vi lập lịch phải có giờ mở cửa và đóng cửa hợp lệ",
                    "/admin/cinemas/" + cinemaPublicId + "/operating-hours"));
        }

        PlanningFacts planningFacts = loadPlanningFacts(cinema, auditoriums, operatingWindows);
        PricingFacts pricingFacts = evaluatePricing(cinema, auditoriums, versions, compatiblePairs, operatingWindows);
        if (pricingFacts.hasMissing()) {
            blockers.add(blocker("PRICING_INCOMPLETE",
                    "Bảng giá hiện tại chưa bao phủ tất cả phòng chiếu và khung giờ có thể xếp lịch",
                    "/admin/pricing-policies?cinemaId=" + cinemaPublicId));
        }
        if (pricingFacts.hasAmbiguous()) {
            blockers.add(blocker("PRICING_AMBIGUOUS",
                    "Có nhiều quy tắc giá cùng mức ưu tiên cao nhất; cần điều chỉnh để chỉ còn một quy tắc áp dụng",
                    "/admin/pricing-policies?cinemaId=" + cinemaPublicId));
        }

        if (!compatiblePairs.isEmpty() && !operatingWindows.isEmpty()
                && !hasAnyAvailableSlot(auditoriums, versions, compatiblePairs,
                operatingWindows, planningFacts)) {
            blockers.add(blocker("PLANNING_RANGE_FULLY_BLOCKED",
                    "Tất cả khung giờ khả dụng đều đang bị chặn bởi lịch chiếu hiện có, thời gian rạp đóng cửa hoặc lịch bảo trì",
                    "/admin/showtimes?cinemaId=" + cinemaPublicId));
        }

        String eligibilityFingerprint = eligibilityFingerprint(
                cinema, versions, auditoriums, compatiblePairs, operatingHours,
                operatingWindows, planningFacts);
        String pricingFingerprint = sha256(String.join("\n", pricingFacts.fingerprintFacts()));
        String configurationFingerprint = sha256(eligibilityFingerprint + "|" + pricingFingerprint);
        int movieCount = (int) versions.stream().map(version -> version.getMovie().getId()).distinct().count();

        AutoSchedulePreflightResponse response = new AutoSchedulePreflightResponse(
                blockers.isEmpty(),
                planningFrom,
                planningTo,
                zoneId.getId(),
                movieCount,
                versions.size(),
                auditoriums.size(),
                compatiblePairs.size(),
                List.copyOf(blockers),
                eligibilityFingerprint,
                pricingFingerprint,
                configurationFingerprint,
                versions.stream().map(MovieVersion::getPublicId).sorted().toList(),
                auditoriums.stream().map(Auditorium::getPublicId).sorted().toList());
        metrics.recordPreflight(Duration.ofNanos(System.nanoTime() - started), response);
        return new AutoSchedulePreflightResult(
                response, cinema, List.copyOf(auditoriums), List.copyOf(versions),
                Set.copyOf(compatiblePairs));
    }

    private List<MovieVersion> filterVersions(List<MovieVersion> source,
                                              Set<String> includes,
                                              Set<String> excludes) {
        return source.stream()
                .filter(version -> includes.isEmpty() || includes.contains(version.getPublicId()))
                .filter(version -> !excludes.contains(version.getPublicId()))
                .sorted(Comparator.comparing(MovieVersion::getPublicId))
                .toList();
    }

    private List<Auditorium> filterAuditoriums(List<Auditorium> source,
                                               Set<String> includes,
                                               Set<String> excludes) {
        return source.stream()
                .filter(auditorium -> auditorium.getCapacity() != null && auditorium.getCapacity() > 0)
                .filter(auditorium -> auditorium.getCleaningBufferMinutes() != null
                        && auditorium.getCleaningBufferMinutes() >= 0)
                .filter(auditorium -> includes.isEmpty() || includes.contains(auditorium.getPublicId()))
                .filter(auditorium -> !excludes.contains(auditorium.getPublicId()))
                .sorted(Comparator.comparing(Auditorium::getPublicId))
                .toList();
    }

    private Set<AutoSchedulePreflightResult.CompatiblePair> compatiblePairs(
            List<MovieVersion> versions,
            List<Auditorium> auditoriums) {
        Set<AutoSchedulePreflightResult.CompatiblePair> result = new LinkedHashSet<>();
        for (MovieVersion version : versions) {
            for (Auditorium auditorium : auditoriums) {
                if (compatibilityPolicy.isCompatible(version.getFormat(), auditorium.getScreenType())) {
                    result.add(new AutoSchedulePreflightResult.CompatiblePair(
                            version.getId(), auditorium.getId()));
                }
            }
        }
        return result;
    }

    private PlanningFacts loadPlanningFacts(Cinema cinema,
                                            List<Auditorium> auditoriums,
                                            List<OperatingWindow> windows) {
        if (auditoriums.isEmpty() || windows.isEmpty()) {
            return PlanningFacts.empty();
        }
        Instant planningStart = windows.stream().map(OperatingWindow::getOpenInstant)
                .min(Comparator.naturalOrder()).orElseThrow();
        Instant latestClose = windows.stream().map(OperatingWindow::getCloseInstant)
                .max(Comparator.naturalOrder()).orElseThrow();
        int maxBuffer = auditoriums.stream().map(Auditorium::getCleaningBufferMinutes)
                .max(Integer::compareTo).orElse(0);
        Instant planningEnd = latestClose.plus(maxBuffer, ChronoUnit.MINUTES);
        Instant lowerBound = planningStart.minus(maxBuffer + 31L, ChronoUnit.MINUTES);
        List<Long> auditoriumIds = auditoriums.stream().map(Auditorium::getId).toList();
        return new PlanningFacts(
                closureRepository.findOverlappingClosures(cinema.getId(), planningStart, planningEnd),
                maintenanceRepository.findActiveOverlapsForAutoSchedule(
                        auditoriumIds, ActionStatus.ACTIVE, planningStart, planningEnd),
                showtimeRepository.findBlockingFactsForAutoSchedule(
                        auditoriumIds, lowerBound, planningEnd));
    }

    private PricingFacts evaluatePricing(Cinema cinema,
                                         List<Auditorium> auditoriums,
                                         List<MovieVersion> versions,
                                         Set<AutoSchedulePreflightResult.CompatiblePair> compatiblePairs,
                                         List<OperatingWindow> windows) {
        Map<Long, Integer> minimumDuration = new HashMap<>();
        Map<Long, MovieVersion> versionsById = new HashMap<>();
        versions.forEach(version -> versionsById.put(version.getId(), version));
        for (AutoSchedulePreflightResult.CompatiblePair pair : compatiblePairs) {
            MovieVersion version = versionsById.get(pair.movieVersionId());
            if (version != null) {
                minimumDuration.merge(pair.auditoriumId(),
                        version.getMovie().getDurationMinutes(), Math::min);
            }
        }

        List<Showtime> probes = new ArrayList<>();
        for (Auditorium auditorium : auditoriums) {
            Integer duration = minimumDuration.get(auditorium.getId());
            if (duration == null) continue;
            for (OperatingWindow window : windows) {
                Instant cursor = window.getOpenInstant();
                while (!cursor.plus(duration, ChronoUnit.MINUTES).isAfter(window.getCloseInstant())) {
                    Showtime probe = new Showtime();
                    probe.setCinema(cinema);
                    probe.setAuditorium(auditorium);
                    probe.setStartTime(cursor);
                    probe.setEndTime(cursor.plus(duration, ChronoUnit.MINUTES));
                    probe.setServiceDate(window.getServiceDate());
                    probes.add(probe);
                    cursor = cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES);
                }
            }
        }
        if (probes.isEmpty()) return PricingFacts.empty();

        List<PriceResolutionResult> resolutions = pricePolicyResolver.resolveAll(probes);
        boolean missing = false;
        boolean ambiguous = false;
        List<String> facts = new ArrayList<>(resolutions.size());
        for (int index = 0; index < resolutions.size(); index++) {
            Showtime probe = probes.get(index);
            PriceResolutionResult result = resolutions.get(index);
            boolean probeMissing = result.resolvedPrices().isEmpty() || !result.missingSeatTypes().isEmpty();
            boolean probeAmbiguous = !result.ambiguousSeatTypes().isEmpty();
            missing |= probeMissing;
            ambiguous |= probeAmbiguous;
            String resolved = result.resolvedPrices().stream()
                    .map(price -> price.seatType().getPublicId() + ":"
                            + price.policy().getPublicId() + ":"
                            + price.rule().getPublicId() + ":" + price.price().toPlainString())
                    .sorted().reduce((left, right) -> left + "," + right).orElse("-");
            facts.add(probe.getAuditorium().getPublicId() + "|" + probe.getStartTime()
                    + "|" + resolved + "|missing=" + probeMissing + "|ambiguous=" + probeAmbiguous);
        }
        return new PricingFacts(missing, ambiguous, List.copyOf(facts));
    }

    private boolean hasAnyAvailableSlot(List<Auditorium> auditoriums,
                                        List<MovieVersion> versions,
                                        Set<AutoSchedulePreflightResult.CompatiblePair> compatiblePairs,
                                        List<OperatingWindow> windows,
                                        PlanningFacts facts) {
        Map<Long, Auditorium> auditoriumsById = new HashMap<>();
        auditoriums.forEach(auditorium -> auditoriumsById.put(auditorium.getId(), auditorium));
        Map<Long, MovieVersion> versionsById = new HashMap<>();
        versions.forEach(version -> versionsById.put(version.getId(), version));
        for (AutoSchedulePreflightResult.CompatiblePair pair : compatiblePairs) {
            Auditorium auditorium = auditoriumsById.get(pair.auditoriumId());
            MovieVersion version = versionsById.get(pair.movieVersionId());
            if (auditorium == null || version == null) continue;
            Movie movie = version.getMovie();
            for (OperatingWindow window : windows) {
                if (!isWithinReleaseWindow(movie, window.getServiceDate())) continue;
                Instant cursor = window.getOpenInstant();
                Instant showtimeEnd = cursor.plus(movie.getDurationMinutes(), ChronoUnit.MINUTES);
                while (!showtimeEnd.isAfter(window.getCloseInstant())) {
                    Instant occupancyEnd = showtimeEnd.plus(
                            auditorium.getCleaningBufferMinutes(), ChronoUnit.MINUTES);
                    if (!overlapsAny(cursor, occupancyEnd, facts.closures())
                            && !overlapsMaintenance(auditorium.getId(), cursor, occupancyEnd, facts.maintenance())
                            && !overlapsShowtimes(auditorium.getId(), cursor, occupancyEnd, facts.showtimes())) {
                        return true;
                    }
                    cursor = cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES);
                    showtimeEnd = cursor.plus(movie.getDurationMinutes(), ChronoUnit.MINUTES);
                }
            }
        }
        return false;
    }

    private boolean isWithinReleaseWindow(Movie movie, LocalDate serviceDate) {
        return (movie.getReleaseDate() == null || !serviceDate.isBefore(movie.getReleaseDate()))
                && (movie.getEndDate() == null || !serviceDate.isAfter(movie.getEndDate()));
    }

    private boolean overlapsAny(Instant start, Instant end, List<CinemaClosurePeriod> closures) {
        return closures.stream().anyMatch(item -> overlaps(start, end, item.getStartTime(), item.getEndTime()));
    }

    private boolean overlapsMaintenance(Long auditoriumId, Instant start, Instant end,
                                        List<AuditoriumMaintenanceWindow> windows) {
        return windows.stream().anyMatch(item -> item.getAuditorium().getId().equals(auditoriumId)
                && overlaps(start, end, item.getStartTime(), item.getEndTime()));
    }

    private boolean overlapsShowtimes(Long auditoriumId, Instant start, Instant end,
                                     List<Showtime> showtimes) {
        return showtimes.stream().anyMatch(item -> item.getAuditorium().getId().equals(auditoriumId)
                && overlaps(start, end, item.getStartTime(), item.getEndTime()));
    }

    private boolean overlaps(Instant firstStart, Instant firstEnd, Instant secondStart, Instant secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private String eligibilityFingerprint(Cinema cinema,
                                          List<MovieVersion> versions,
                                          List<Auditorium> auditoriums,
                                          Set<AutoSchedulePreflightResult.CompatiblePair> pairs,
                                          List<CinemaOperatingHour> hours,
                                          List<OperatingWindow> windows,
                                          PlanningFacts planningFacts) {
        TreeSet<String> facts = new TreeSet<>();
        facts.add("cinema|" + cinema.getPublicId() + "|" + cinema.getStatus() + "|"
                + cinema.getTimezone() + "|engine=" + cinema.getAutoScheduleEngine()
                + "|" + cinema.getUpdatedAt());
        versions.forEach(version -> facts.add("version|" + version.getPublicId() + "|"
                + version.getStatus() + "|" + version.getFormat() + "|" + version.getUpdatedAt()
                + "|movie=" + version.getMovie().getPublicId() + ":"
                + version.getMovie().getStatus() + ":" + version.getMovie().getDurationMinutes()
                + ":" + version.getMovie().getReleaseDate() + ":" + version.getMovie().getEndDate()
                + ":" + version.getMovie().getVersion()));
        auditoriums.forEach(auditorium -> facts.add("auditorium|" + auditorium.getPublicId() + "|"
                + auditorium.getStatus() + "|" + auditorium.getScreenType() + "|"
                + auditorium.getCapacity() + "|" + auditorium.getCleaningBufferMinutes()
                + "|" + auditorium.getUpdatedAt()));
        pairs.forEach(pair -> facts.add("pair|" + pair.movieVersionId() + "|" + pair.auditoriumId()));
        hours.forEach(hour -> facts.add("hours|" + hour.getDayOfWeek() + "|" + hour.getOpenTime()
                + "|" + hour.getCloseTime() + "|" + hour.getIsClosed() + "|" + hour.getUpdatedAt()));
        windows.forEach(window -> facts.add("window|" + window.getServiceDate() + "|"
                + window.getOpenInstant() + "|" + window.getCloseInstant()));
        planningFacts.closures().forEach(item -> facts.add("closure|" + item.getId() + "|"
                + item.getStartTime() + "|" + item.getEndTime() + "|" + item.getUpdatedAt()));
        planningFacts.maintenance().forEach(item -> facts.add("maintenance|" + item.getId() + "|"
                + item.getAuditorium().getId() + "|" + item.getStartTime() + "|"
                + item.getEndTime() + "|" + item.getUpdatedAt()));
        planningFacts.showtimes().forEach(item -> facts.add("showtime|" + item.getPublicId() + "|"
                + item.getAuditorium().getId() + "|" + item.getStartTime() + "|"
                + item.getEndTime() + "|" + item.getVersion()));
        return sha256(String.join("\n", facts));
    }

    private AutoSchedulePreflightResponse.Blocker blocker(String code, String message, String actionPath) {
        return new AutoSchedulePreflightResponse.Blocker(code, message, actionPath);
    }

    private Set<String> normalizedSet(List<String> values) {
        if (values == null) return Set.of();
        Set<String> result = new HashSet<>();
        values.stream().map(this::trim).filter(value -> value != null && !value.isBlank())
                .forEach(result::add);
        return Set.copyOf(result);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record PlanningFacts(List<CinemaClosurePeriod> closures,
                                 List<AuditoriumMaintenanceWindow> maintenance,
                                 List<Showtime> showtimes) {
        private static PlanningFacts empty() {
            return new PlanningFacts(List.of(), List.of(), List.of());
        }
    }

    private record PricingFacts(boolean hasMissing,
                                boolean hasAmbiguous,
                                List<String> fingerprintFacts) {
        private static PricingFacts empty() {
            return new PricingFacts(false, false, List.of("no-pricing-probes"));
        }
    }
}
