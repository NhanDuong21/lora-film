package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.repository.PricePolicyRepository;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PricePolicyResolverImpl implements PricePolicyResolver {

    private final PricePolicyRepository pricePolicyRepository;
    private final SeatRepository seatRepository;
    private final Clock clock;

    public PricePolicyResolverImpl(PricePolicyRepository pricePolicyRepository,
                                   SeatRepository seatRepository,
                                   Clock clock) {
        this.pricePolicyRepository = pricePolicyRepository;
        this.seatRepository = seatRepository;
        this.clock = clock;
    }

    @Override
    public PriceResolutionResult resolve(Showtime showtime) {
        String timezone = showtime.getCinema().getTimezone();
        ZoneId zoneId = parseZoneId(timezone);
        ZonedDateTime localStart = showtime.getStartTime().atZone(zoneId);
        List<SeatType> requiredSeatTypes =
                seatRepository.findActiveSeatTypesByAuditoriumId(showtime.getAuditorium().getId());
        List<PricePolicy> policies = pricePolicyRepository.findEffectiveActivePolicies(
                showtime.getCinema().getId(), localStart.toLocalDate());
        return resolveWithFacts(showtime, zoneId, localStart, requiredSeatTypes, policies);
    }

    @Override
    public List<PriceResolutionResult> resolveAll(List<Showtime> showtimes) {
        Map<Long, List<SeatType>> seatTypesByAuditorium = new HashMap<>();
        Map<Long, LocalDateRange> rangesByCinema = new HashMap<>();
        for (Showtime showtime : showtimes) {
            ZoneId zoneId = parseZoneId(showtime.getCinema().getTimezone());
            LocalDate localDate = showtime.getStartTime().atZone(zoneId).toLocalDate();
            rangesByCinema.compute(
                    showtime.getCinema().getId(),
                    (ignored, range) -> range == null ? new LocalDateRange(localDate, localDate)
                            : range.include(localDate));
        }
        List<Long> auditoriumIds = showtimes.stream()
                .map(showtime -> showtime.getAuditorium().getId())
                .distinct().sorted().toList();
        if (!auditoriumIds.isEmpty()) {
            for (Object[] row : seatRepository.findActiveSeatTypesByAuditoriumIds(auditoriumIds)) {
                seatTypesByAuditorium.computeIfAbsent((Long) row[0], ignored -> new ArrayList<>())
                        .add((SeatType) row[1]);
            }
            auditoriumIds.forEach(id -> seatTypesByAuditorium.putIfAbsent(id, List.of()));
        }
        Map<Long, List<PricePolicy>> policiesByCinema = new HashMap<>();
        rangesByCinema.forEach((cinemaId, range) -> policiesByCinema.put(
                cinemaId,
                pricePolicyRepository.findActivePoliciesOverlappingDateRange(
                        cinemaId, range.fromInclusive(), range.toInclusive())));
        List<PriceResolutionResult> results = new ArrayList<>();

        for (Showtime showtime : showtimes) {
            ZoneId zoneId = parseZoneId(showtime.getCinema().getTimezone());
            ZonedDateTime localStart = showtime.getStartTime().atZone(zoneId);
            List<SeatType> requiredSeatTypes = seatTypesByAuditorium
                    .getOrDefault(showtime.getAuditorium().getId(), List.of());
            List<PricePolicy> policies = policiesByCinema
                    .getOrDefault(showtime.getCinema().getId(), List.of())
                    .stream()
                    .filter(policy -> isEffective(policy, localStart.toLocalDate()))
                    .toList();
            results.add(resolveWithFacts(showtime, zoneId, localStart, requiredSeatTypes, policies));
        }
        return List.copyOf(results);
    }

    private boolean isEffective(PricePolicy policy, LocalDate localDate) {
        return !policy.getEffectiveFrom().isAfter(localDate)
                && (policy.getEffectiveTo() == null || !policy.getEffectiveTo().isBefore(localDate));
    }

    private PriceResolutionResult resolveWithFacts(Showtime showtime,
                                                   ZoneId zoneId,
                                                   ZonedDateTime localStart,
                                                   List<SeatType> requiredSeatTypes,
                                                   List<PricePolicy> policies) {
        List<PriceResolutionResult.ResolvedPrice> resolved = new ArrayList<>();
        List<PriceResolutionResult.SeatTypeDiagnostic> missing = new ArrayList<>();
        List<PriceResolutionResult.SeatTypeDiagnostic> ambiguous = new ArrayList<>();
        String currency = null;

        for (SeatType seatType : requiredSeatTypes) {
            List<Candidate> candidates = collectCandidates(showtime, localStart, seatType, policies);
            if (candidates.isEmpty() && seatType.getCode() == SeatTypeCode.DISABLED) {
                candidates = requiredSeatTypes.stream()
                        .filter(required -> required.getCode() == SeatTypeCode.STANDARD)
                        .findFirst()
                        .map(standard -> collectCandidates(showtime, localStart, standard, policies))
                        .orElseGet(List::of);
            }
            if (candidates.isEmpty()) {
                missing.add(diagnostic(seatType, List.of()));
                continue;
            }

            candidates.sort(Comparator.comparing(Candidate::rank).reversed()
                    .thenComparing(candidate -> candidate.rule().getPublicId()));
            ResolutionRank highestRank = candidates.get(0).rank();
            List<Candidate> winners = candidates.stream()
                    .filter(candidate -> candidate.rank().equals(highestRank))
                    .toList();

            if (winners.size() != 1) {
                ambiguous.add(diagnostic(seatType, winners.stream()
                        .map(candidate -> candidate.rule().getPublicId())
                        .sorted()
                        .toList()));
                continue;
            }

            Candidate winner = winners.get(0);
            if (currency == null) {
                currency = winner.policy().getCurrency();
            } else if (!currency.equals(winner.policy().getCurrency())) {
                ambiguous.add(diagnostic(seatType, List.of(winner.rule().getPublicId())));
                continue;
            }
            resolved.add(new PriceResolutionResult.ResolvedPrice(
                    seatType, winner.rule().getPrice(), winner.policy(), winner.rule()));
        }

        return new PriceResolutionResult(
                currency == null ? "VND" : currency,
                zoneId.getId(),
                Instant.now(clock),
                List.copyOf(resolved),
                List.copyOf(missing),
                List.copyOf(ambiguous));
    }

    private List<Candidate> collectCandidates(Showtime showtime,
                                              ZonedDateTime localStart,
                                              SeatType seatType,
                                              List<PricePolicy> policies) {
        List<Candidate> candidates = new ArrayList<>();
        for (PricePolicy policy : policies) {
            for (PricePolicyRule rule : policy.getRules()) {
                if (!rule.isActive()
                        || rule.getDeletedAt() != null
                        || !rule.getSeatType().getId().equals(seatType.getId())
                        || !rule.getDayType().matches(localStart.getDayOfWeek())
                        || !matchesScope(rule, showtime)
                        || !matchesTime(rule, localStart.toLocalTime())) {
                    continue;
                }
                candidates.add(new Candidate(
                        policy,
                        rule,
                        new ResolutionRank(
                                scopeRank(rule),
                                rule.getDayType().rank(),
                                rule.isBoundedTimeBand() ? 2 : 1,
                                policy.getPriority())));
            }
        }
        return candidates;
    }

    private boolean matchesScope(PricePolicyRule rule, Showtime showtime) {
        if (rule.getAuditorium() != null) {
            return rule.getAuditorium().getId().equals(showtime.getAuditorium().getId());
        }
        if (rule.getScreenType() != null) {
            return rule.getScreenType() == showtime.getAuditorium().getScreenType();
        }
        return true;
    }

    static boolean matchesTime(PricePolicyRule rule, LocalTime localTime) {
        if (!rule.isBoundedTimeBand()) {
            return true;
        }
        LocalTime start = rule.getTimeBandStart();
        LocalTime end = rule.getTimeBandEnd();
        if (start.isBefore(end)) {
            return !localTime.isBefore(start) && localTime.isBefore(end);
        }
        return !localTime.isBefore(start) || localTime.isBefore(end);
    }

    private int scopeRank(PricePolicyRule rule) {
        if (rule.getAuditorium() != null) {
            return 3;
        }
        if (rule.getScreenType() != null) {
            return 2;
        }
        return 1;
    }

    private PriceResolutionResult.SeatTypeDiagnostic diagnostic(SeatType seatType, List<String> candidates) {
        return new PriceResolutionResult.SeatTypeDiagnostic(
                seatType.getPublicId(),
                seatType.getCode().name(),
                seatType.getName(),
                candidates);
    }

    private ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE,
                    "Cinema timezone is required for price resolution");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE,
                    "Invalid cinema timezone: " + timezone);
        }
    }

    private record Candidate(PricePolicy policy, PricePolicyRule rule, ResolutionRank rank) {
    }

    private record LocalDateRange(LocalDate fromInclusive, LocalDate toInclusive) {
        private LocalDateRange include(LocalDate localDate) {
            return new LocalDateRange(
                    localDate.isBefore(fromInclusive) ? localDate : fromInclusive,
                    localDate.isAfter(toInclusive) ? localDate : toInclusive);
        }
    }

    private record ResolutionRank(int scope, int day, int time, int priority)
            implements Comparable<ResolutionRank> {
        @Override
        public int compareTo(ResolutionRank other) {
            int comparison = Integer.compare(scope, other.scope);
            if (comparison == 0) comparison = Integer.compare(day, other.day);
            if (comparison == 0) comparison = Integer.compare(time, other.time);
            if (comparison == 0) comparison = Integer.compare(priority, other.priority);
            return comparison;
        }
    }
}
