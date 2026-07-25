package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.dto.PageResponse;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import com.lorafilm.movie.pricing.dto.request.ActivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CopyPricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.CreatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.DeactivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.PricePolicyRuleRequest;
import com.lorafilm.movie.pricing.dto.request.PriceResolutionPreviewRequest;
import com.lorafilm.movie.pricing.dto.request.UpdatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.response.PricePolicyResponse;
import com.lorafilm.movie.pricing.dto.response.PricePolicyRuleResponse;
import com.lorafilm.movie.pricing.dto.response.PricePolicyUsageResponse;
import com.lorafilm.movie.pricing.dto.response.PriceResolutionPreviewResponse;
import com.lorafilm.movie.pricing.dto.response.PriceSeatTypeDiagnosticDto;
import com.lorafilm.movie.pricing.repository.PricePolicyRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.service.PricePolicyOverlapValidator;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.PricePolicyService;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PricePolicyServiceImpl implements PricePolicyService {

    private final PricePolicyRepository policyRepository;
    private final CinemaRepository cinemaRepository;
    private final AuditoriumRepository auditoriumRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;
    private final PricePolicyOverlapValidator overlapValidator;
    private final PricePolicyResolver resolver;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public PricePolicyServiceImpl(PricePolicyRepository policyRepository,
                                  CinemaRepository cinemaRepository,
                                  AuditoriumRepository auditoriumRepository,
                                  SeatTypeRepository seatTypeRepository,
                                  ShowtimeRepository showtimeRepository,
                                  ShowtimePriceRepository showtimePriceRepository,
                                  PricePolicyOverlapValidator overlapValidator,
                                  PricePolicyResolver resolver,
                                  CurrentUserProvider currentUserProvider,
                                  Clock clock) {
        this.policyRepository = policyRepository;
        this.cinemaRepository = cinemaRepository;
        this.auditoriumRepository = auditoriumRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.showtimeRepository = showtimeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
        this.overlapValidator = overlapValidator;
        this.resolver = resolver;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PricePolicyResponse> search(String cinemaId,
                                                    String status,
                                                    LocalDate effectiveDate,
                                                    int page,
                                                    int size) {
        String normalizedStatus = blankToNull(status) == null
                ? null : status.trim().toUpperCase();
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        if ("EXPIRED".equals(normalizedStatus)) {
            List<PricePolicyResponse> expired = policyRepository
                    .findActiveDisplayCandidates(blankToNull(cinemaId), effectiveDate)
                    .stream()
                    .filter(policy -> "EXPIRED".equals(displayStatus(policy)))
                    .map(policy -> toResponse(policy, List.of(), false))
                    .toList();
            int from = (int) Math.min((long) normalizedPage * normalizedSize, expired.size());
            int to = Math.min(from + normalizedSize, expired.size());
            int totalPages = expired.isEmpty() ? 0
                    : (expired.size() + normalizedSize - 1) / normalizedSize;
            return new PageResponse<>(expired.subList(from, to), normalizedPage, normalizedSize,
                    expired.size(), totalPages, normalizedPage + 1 >= totalPages);
        }
        PricePolicyStatus storedStatus = parseStoredStatus(normalizedStatus);
        Page<PricePolicy> result = policyRepository.search(
                blankToNull(cinemaId),
                storedStatus,
                effectiveDate,
                PageRequest.of(normalizedPage, normalizedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PricePolicyResponse> content = result.getContent().stream()
                .map(policy -> toResponse(policy, List.of(), false))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Override
    @Transactional
    public PricePolicyResponse create(CreatePricePolicyRequest request) {
        PricePolicy policy = new PricePolicy();
        policy.setPublicId(UUID.randomUUID().toString());
        policy.setStatus(PricePolicyStatus.DRAFT);
        apply(policy, request.name(), request.cinemaId(), request.effectiveFrom(),
                request.effectiveTo(), request.currency(), request.priority(), request.rules());
        List<PricePolicyOverlapValidator.Conflict> conflicts = draftConflicts(policy);
        return saveAndMap(policy, conflicts);
    }

    @Override
    @Transactional(readOnly = true)
    public PricePolicyResponse get(String publicId) {
        PricePolicy policy = find(publicId);
        return toResponse(policy, draftConflicts(policy), true);
    }

    @Override
    @Transactional
    public PricePolicyResponse update(String publicId, UpdatePricePolicyRequest request) {
        PricePolicy policy = lock(publicId);
        requireDraft(policy);
        requireVersion(policy, request.expectedVersion());
        apply(policy, request.name(), request.cinemaId(), request.effectiveFrom(),
                request.effectiveTo(), request.currency(), request.priority(), request.rules());
        List<PricePolicyOverlapValidator.Conflict> conflicts = draftConflicts(policy);
        return saveAndMap(policy, conflicts);
    }

    @Override
    @Transactional
    public PricePolicyResponse activate(String publicId, ActivatePricePolicyRequest request) {
        PricePolicy policy = lock(publicId);
        requireDraft(policy);
        requireVersion(policy, request.expectedVersion());
        validatePolicy(policy, true);
        List<PricePolicyOverlapValidator.Conflict> conflicts = draftConflicts(policy);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ErrorCode.PRICE_POLICY_OVERLAP,
                    "Policy activation would create equal-rank price ambiguity", conflicts);
        }
        policy.setStatus(PricePolicyStatus.ACTIVE);
        policy.setActivatedAt(Instant.now(clock));
        policy.setActivatedBy(requireActor());
        return saveAndMap(policy, List.of());
    }

    @Override
    @Transactional
    public PricePolicyResponse deactivate(String publicId, DeactivatePricePolicyRequest request) {
        PricePolicy policy = lock(publicId);
        requireVersion(policy, request.expectedVersion());
        if (policy.getStatus() != PricePolicyStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.PRICE_POLICY_IMMUTABLE,
                    "Only an ACTIVE policy can be deactivated");
        }
        policy.setStatus(PricePolicyStatus.INACTIVE);
        policy.setDeactivatedAt(Instant.now(clock));
        policy.setDeactivatedBy(requireActor());
        policy.setDeactivationReason(request.reason().trim());
        return saveAndMap(policy, List.of());
    }

    @Override
    @Transactional
    public PricePolicyResponse copy(String publicId, CopyPricePolicyRequest request) {
        PricePolicy source = lock(publicId);
        requireVersion(source, request.expectedVersion());
        PricePolicy copy = new PricePolicy();
        copy.setPublicId(UUID.randomUUID().toString());
        copy.setName(request.name().trim());
        copy.setCinema(source.getCinema());
        copy.setEffectiveFrom(source.getEffectiveFrom());
        copy.setEffectiveTo(source.getEffectiveTo());
        copy.setCurrency(source.getCurrency());
        copy.setPriority(source.getPriority());
        copy.setStatus(PricePolicyStatus.DRAFT);
        copy.setSupersedesPolicy(source);
        for (PricePolicyRule sourceRule : source.getRules()) {
            PricePolicyRule rule = new PricePolicyRule();
            rule.setPublicId(UUID.randomUUID().toString());
            rule.setSeatType(sourceRule.getSeatType());
            rule.setAuditorium(sourceRule.getAuditorium());
            rule.setScreenType(sourceRule.getScreenType());
            rule.setDayType(sourceRule.getDayType());
            rule.setTimeBandStart(sourceRule.getTimeBandStart());
            rule.setTimeBandEnd(sourceRule.getTimeBandEnd());
            rule.setPrice(sourceRule.getPrice());
            rule.setActive(sourceRule.isActive());
            copy.addRule(rule);
        }
        return saveAndMap(copy, draftConflicts(copy));
    }

    @Override
    @Transactional(readOnly = true)
    public PricePolicyUsageResponse usage(String publicId, int page, int size) {
        PricePolicy policy = find(publicId);
        ZoneId zoneId = parseZone(policy.getCinema().getTimezone());
        Instant now = Instant.now(clock);
        Instant policyStart = policy.getEffectiveFrom().atStartOfDay(zoneId).toInstant();
        Instant from = now.isAfter(policyStart) ? now : policyStart;
        Instant to = policy.getEffectiveTo() == null
                ? null : policy.getEffectiveTo().plusDays(1).atStartOfDay(zoneId).toInstant();
        Page<Showtime> affected = showtimeRepository.findFutureDraftsForPricingPolicy(
                policy.getCinema().getId(), from, to,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<PricePolicyUsageResponse.AffectedShowtime> items = affected.getContent().stream()
                .map(showtime -> new PricePolicyUsageResponse.AffectedShowtime(
                        showtime.getPublicId(),
                        showtime.getAuditorium().getPublicId(),
                        showtime.getAuditorium().getName(),
                        showtime.getStartTime()))
                .toList();
        return new PricePolicyUsageResponse(
                showtimePriceRepository.countDistinctShowtimesBySourcePolicyPublicId(publicId),
                affected.getTotalElements(),
                items,
                affected.getNumber(),
                affected.getSize(),
                affected.getTotalPages(),
                affected.isLast());
    }

    @Override
    @Transactional(readOnly = true)
    public PriceResolutionPreviewResponse preview(PriceResolutionPreviewRequest request) {
        Showtime showtime;
        if (request.showtimeId() != null && !request.showtimeId().isBlank()) {
            showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(request.showtimeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND));
        } else {
            if (request.cinemaId() == null || request.auditoriumId() == null || request.startTime() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Provide showtimeId or cinemaId, auditoriumId, and startTime");
            }
            Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(request.cinemaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
            Auditorium auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(request.auditoriumId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));
            if (!auditorium.getCinema().getId().equals(cinema.getId())) {
                throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA);
            }
            showtime = new Showtime();
            showtime.setCinema(cinema);
            showtime.setAuditorium(auditorium);
            showtime.setStartTime(request.startTime());
        }
        return toPreview(resolver.resolve(showtime));
    }

    private void apply(PricePolicy policy,
                       String name,
                       String cinemaPublicId,
                       LocalDate effectiveFrom,
                       LocalDate effectiveTo,
                       String currency,
                       Integer priority,
                       List<PricePolicyRuleRequest> rules) {
        Cinema cinema = cinemaRepository.findByPublicIdAndDeletedAtIsNull(cinemaPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CINEMA_NOT_FOUND));
        policy.setName(name.trim());
        policy.setCinema(cinema);
        policy.setEffectiveFrom(effectiveFrom);
        policy.setEffectiveTo(effectiveTo);
        policy.setCurrency(currency == null ? null : currency.trim().toUpperCase());
        policy.setPriority(priority);
        validatePolicy(policy, false);
        policy.replaceRules(mapRules(policy, rules));
    }

    private List<PricePolicyRule> mapRules(PricePolicy policy, List<PricePolicyRuleRequest> requests) {
        List<PricePolicyRule> result = new ArrayList<>();
        for (PricePolicyRuleRequest request : requests) {
            SeatType seatType = seatTypeRepository.findByPublicIdAndDeletedAtIsNull(request.seatTypeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND,
                            "SeatType not found: " + request.seatTypeId()));
            if (seatType.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE,
                        "SeatType is inactive: " + request.seatTypeId());
            }
            if (request.auditoriumId() != null && request.screenType() != null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Auditorium and ScreenType scopes cannot both be set");
            }
            Auditorium auditorium = null;
            if (request.auditoriumId() != null && !request.auditoriumId().isBlank()) {
                auditorium = auditoriumRepository.findByPublicIdAndDeletedAtIsNull(request.auditoriumId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.AUDITORIUM_NOT_FOUND));
                if (!auditorium.getCinema().getId().equals(policy.getCinema().getId())) {
                    throw new BusinessException(ErrorCode.AUDITORIUM_NOT_BELONG_TO_CINEMA);
                }
            }
            validateTimeBand(request);
            if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PRICE_INVALID, "Rule price must be positive");
            }
            PricePolicyRule rule = new PricePolicyRule();
            rule.setPublicId(UUID.randomUUID().toString());
            rule.setSeatType(seatType);
            rule.setAuditorium(auditorium);
            rule.setScreenType(request.screenType());
            rule.setDayType(request.dayType());
            rule.setTimeBandStart(request.timeBandStart());
            rule.setTimeBandEnd(request.timeBandEnd());
            rule.setPrice(request.price());
            rule.setActive(request.active() == null || request.active());
            result.add(rule);
        }
        return result;
    }

    private void validateTimeBand(PricePolicyRuleRequest request) {
        boolean hasStart = request.timeBandStart() != null;
        boolean hasEnd = request.timeBandEnd() != null;
        if (hasStart != hasEnd) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Time-band start and end must both be set or both be null");
        }
        if (hasStart && request.timeBandStart().equals(request.timeBandEnd())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Time-band start and end cannot be equal");
        }
    }

    private void validatePolicy(PricePolicy policy, boolean requireRules) {
        if (policy.getEffectiveFrom() == null
                || (policy.getEffectiveTo() != null
                    && policy.getEffectiveTo().isBefore(policy.getEffectiveFrom()))) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Effective end date cannot be before the start date");
        }
        if (!"VND".equals(policy.getCurrency())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "V1 supports VND only");
        }
        if (policy.getPriority() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Policy priority is required");
        }
        if (requireRules && policy.getRules().stream().noneMatch(PricePolicyRule::isActive)) {
            throw new BusinessException(ErrorCode.PRICING_INCOMPLETE,
                    "An activated policy requires at least one active rule");
        }
    }

    private List<PricePolicyOverlapValidator.Conflict> draftConflicts(PricePolicy policy) {
        if (policy.getStatus() != PricePolicyStatus.DRAFT) {
            return List.of();
        }
        LocalDate toInclusive = policy.getEffectiveTo() == null
                ? LocalDate.of(9999, 12, 31) : policy.getEffectiveTo();
        List<PricePolicy> active = policyRepository.findActivePoliciesOverlappingDateRange(
                policy.getCinema().getId(), policy.getEffectiveFrom(), toInclusive);
        return overlapValidator.findConflicts(policy, active);
    }

    private PricePolicyResponse saveAndMap(PricePolicy policy,
                                           List<PricePolicyOverlapValidator.Conflict> conflicts) {
        try {
            PricePolicy saved = policyRepository.saveAndFlush(policy);
            return toResponse(saved, conflicts, true);
        } catch (OptimisticLockingFailureException exception) {
            throw new BusinessException(ErrorCode.PRICING_CONCURRENT_MODIFICATION,
                    "Price policy was modified by another request");
        }
    }

    private PricePolicyResponse toResponse(PricePolicy policy,
                                           List<PricePolicyOverlapValidator.Conflict> conflicts,
                                           boolean includeRules) {
        List<PricePolicyRuleResponse> rules = includeRules
                ? policy.getRules().stream()
                    .map(rule -> new PricePolicyRuleResponse(
                            rule.getPublicId(),
                            rule.getSeatType().getPublicId(),
                            rule.getSeatType().getCode().name(),
                            rule.getSeatType().getName(),
                            rule.getAuditorium() == null ? null : rule.getAuditorium().getPublicId(),
                            rule.getAuditorium() == null ? null : rule.getAuditorium().getName(),
                            rule.getScreenType() == null ? null : rule.getScreenType().getValue(),
                            rule.getDayType(),
                            rule.getTimeBandStart(),
                            rule.getTimeBandEnd(),
                            rule.getPrice(),
                            rule.isActive()))
                    .toList()
                : List.of();
        return new PricePolicyResponse(
                policy.getPublicId(),
                policy.getName(),
                policy.getCinema().getPublicId(),
                policy.getCinema().getName(),
                policy.getEffectiveFrom(),
                policy.getEffectiveTo(),
                policy.getStatus().name(),
                displayStatus(policy),
                policy.getCurrency(),
                policy.getPriority(),
                policy.getSupersedesPolicy() == null ? null : policy.getSupersedesPolicy().getPublicId(),
                policy.getActivatedAt(),
                policy.getActivatedBy(),
                policy.getDeactivatedAt(),
                policy.getDeactivatedBy(),
                policy.getDeactivationReason(),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                rules,
                conflicts);
    }

    private String displayStatus(PricePolicy policy) {
        if (policy.getStatus() == PricePolicyStatus.ACTIVE && policy.getEffectiveTo() != null) {
            LocalDate localToday = LocalDate.now(clock.withZone(parseZone(policy.getCinema().getTimezone())));
            if (policy.getEffectiveTo().isBefore(localToday)) {
                return "EXPIRED";
            }
        }
        return policy.getStatus().name();
    }

    private PriceResolutionPreviewResponse toPreview(PriceResolutionResult result) {
        return new PriceResolutionPreviewResponse(
                result.isComplete(),
                result.currency(),
                result.timezone(),
                result.resolvedAt(),
                result.resolvedPrices().stream()
                        .map(line -> new PriceResolutionPreviewResponse.ResolvedLine(
                                line.seatType().getPublicId(),
                                line.seatType().getCode().name(),
                                line.seatType().getName(),
                                line.price(),
                                line.policy().getPublicId(),
                                line.policy().getName(),
                                line.rule().getPublicId()))
                        .toList(),
                result.missingSeatTypes().stream().map(this::diagnostic).toList(),
                result.ambiguousSeatTypes().stream().map(this::diagnostic).toList());
    }

    private PriceSeatTypeDiagnosticDto diagnostic(PriceResolutionResult.SeatTypeDiagnostic source) {
        return new PriceSeatTypeDiagnosticDto(
                source.seatTypeId(), source.seatTypeCode(), source.seatTypeName(), source.candidateRuleIds());
    }

    private PricePolicy find(String publicId) {
        return policyRepository.findByPublicIdAndDeletedAtIsNull(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_POLICY_NOT_FOUND));
    }

    private PricePolicy lock(String publicId) {
        return policyRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRICE_POLICY_NOT_FOUND));
    }

    private void requireDraft(PricePolicy policy) {
        if (policy.getStatus() != PricePolicyStatus.DRAFT) {
            throw new BusinessException(ErrorCode.PRICE_POLICY_IMMUTABLE);
        }
    }

    private void requireVersion(PricePolicy policy, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(policy.getVersion())) {
            Map<String, Object> details = new HashMap<>();
            details.put("expectedVersion", expectedVersion);
            details.put("actualVersion", policy.getVersion());
            throw new BusinessException(ErrorCode.PRICING_CONCURRENT_MODIFICATION,
                    "Price policy version does not match", details);
        }
    }

    private Long requireActor() {
        Long actor = currentUserProvider.getCurrentUserId();
        if (actor == null) {
            throw new BusinessException(ErrorCode.CURRENT_USER_NOT_AVAILABLE);
        }
        return actor;
    }

    private ZoneId parseZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException exception) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE,
                    "Invalid cinema timezone: " + timezone);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private PricePolicyStatus parseStoredStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return PricePolicyStatus.valueOf(status);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Unknown policy status: " + status);
        }
    }
}
