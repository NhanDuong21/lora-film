package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.exception.ResourceNotFoundException;
import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import com.lorafilm.movie.pricing.domain.enums.PricingSource;
import com.lorafilm.movie.pricing.dto.request.ShowtimePriceItemRequest;
import com.lorafilm.movie.pricing.dto.request.UpdateShowtimePricesRequest;
import com.lorafilm.movie.pricing.dto.response.PriceSeatTypeDiagnosticDto;
import com.lorafilm.movie.pricing.dto.response.ShowtimePriceDto;
import com.lorafilm.movie.pricing.dto.response.ShowtimePricesResponse;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.pricing.service.ShowtimePricingService;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShowtimePricingServiceImpl implements ShowtimePricingService {

    private static final String SUPPORTED_CURRENCY = "VND";

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimePriceRepository showtimePriceRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final SeatRepository seatRepository;
    private final PricePolicyResolver pricePolicyResolver;

    public ShowtimePricingServiceImpl(ShowtimeRepository showtimeRepository,
                                      ShowtimePriceRepository showtimePriceRepository,
                                      SeatTypeRepository seatTypeRepository,
                                      SeatRepository seatRepository,
                                      PricePolicyResolver pricePolicyResolver) {
        this.showtimeRepository = showtimeRepository;
        this.showtimePriceRepository = showtimePriceRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.seatRepository = seatRepository;
        this.pricePolicyResolver = pricePolicyResolver;
    }

    @Override
    @Transactional
    public ShowtimePricesResponse updatePrices(String showtimePublicId, UpdateShowtimePricesRequest request) {
        Showtime showtime = showtimeRepository.findByPublicIdForUpdate(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        requireDraft(showtime);

        List<String> requiredIds =
                seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(showtime.getAuditorium().getId());
        List<String> requestedIds = request.getPrices().stream()
                .map(ShowtimePriceItemRequest::getSeatTypeId)
                .toList();
        if (requestedIds.stream().distinct().count() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_INVALID, "Duplicate seat type in request");
        }
        if (!new HashSet<>(requestedIds).equals(new HashSet<>(requiredIds))) {
            throw new BusinessException(ErrorCode.PRICING_INCOMPLETE,
                    "Manual override must include exactly every active SeatType in the Auditorium",
                    Map.of("requiredSeatTypeIds", requiredIds, "submittedSeatTypeIds", requestedIds));
        }

        List<SeatType> seatTypes = seatTypeRepository.findAllByPublicIdInAndDeletedAtIsNull(requestedIds);
        if (seatTypes.size() != requestedIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND, "One or more seat types not found");
        }
        Map<String, SeatType> seatTypesByPublicId = seatTypes.stream()
                .collect(Collectors.toMap(SeatType::getPublicId, seatType -> seatType));

        showtimePriceRepository.deleteByShowtimeId(showtime.getId());
        showtimePriceRepository.flush();
        Instant resolvedAt = Instant.now();
        String timezone = requireTimezone(showtime);

        for (ShowtimePriceItemRequest item : request.getPrices()) {
            SeatType seatType = seatTypesByPublicId.get(item.getSeatTypeId());
            if (seatType.getStatus() != ActiveStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.SEAT_TYPE_INACTIVE,
                        "Seat type " + seatType.getPublicId() + " is inactive");
            }
            if (item.getPrice() == null || item.getPrice().signum() <= 0) {
                throw new BusinessException(ErrorCode.PRICE_INVALID, "Showtime prices must be positive");
            }
            showtimePriceRepository.save(newSnapshot(
                    showtime,
                    seatType,
                    item.getPrice(),
                    SUPPORTED_CURRENCY,
                    PricingSource.MANUAL_OVERRIDE,
                    null,
                    null,
                    resolvedAt,
                    timezone));
        }
        showtimePriceRepository.flush();
        return buildResponse(showtime, showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimePricesResponse getPrices(String showtimePublicId) {
        Showtime showtime = showtimeRepository.findByPublicIdAndDeletedAtIsNull(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        return buildResponse(showtime, showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId()), null);
    }

    @Override
    @Transactional
    public ShowtimePricesResponse resolvePrices(String showtimePublicId, Long expectedShowtimeVersion) {
        Showtime showtime = showtimeRepository.findByPublicIdForUpdate(showtimePublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));
        requireDraft(showtime);
        if (expectedShowtimeVersion != null && !expectedShowtimeVersion.equals(showtime.getVersion())) {
            throw new BusinessException(ErrorCode.PRICING_CONCURRENT_MODIFICATION,
                    "Showtime version does not match",
                    Map.of("expectedVersion", expectedShowtimeVersion, "actualVersion", showtime.getVersion()));
        }
        PriceResolutionResult result = resolveAndReplace(showtime);
        return buildResponse(
                showtime,
                showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId()),
                result);
    }

    @Override
    @Transactional
    public PriceResolutionResult resolveAndReplace(Showtime showtime) {
        requireDraft(showtime);
        PriceResolutionResult result = pricePolicyResolver.resolve(showtime);

        showtimePriceRepository.deleteByShowtimeId(showtime.getId());
        showtimePriceRepository.flush();
        if (!result.isComplete()) {
            return result;
        }

        List<ShowtimePrice> snapshots = result.resolvedPrices().stream()
                .map(resolved -> newSnapshot(
                        showtime,
                        resolved.seatType(),
                        resolved.price(),
                        result.currency(),
                        PricingSource.POLICY,
                        resolved.policy(),
                        resolved.rule(),
                        result.resolvedAt(),
                        result.timezone()))
                .toList();
        showtimePriceRepository.saveAll(snapshots);
        showtimePriceRepository.flush();
        return result;
    }

    @Override
    @Transactional
    public List<PriceResolutionResult> resolveAndReplaceAll(List<Showtime> showtimes) {
        if (showtimes == null || showtimes.isEmpty()) {
            return List.of();
        }
        showtimes.forEach(this::requireDraft);
        List<PriceResolutionResult> results = pricePolicyResolver.resolveAll(showtimes);
        for (Showtime showtime : showtimes) {
            showtimePriceRepository.deleteByShowtimeId(showtime.getId());
        }
        showtimePriceRepository.flush();

        List<ShowtimePrice> snapshots = new java.util.ArrayList<>();
        for (int index = 0; index < showtimes.size(); index++) {
            Showtime showtime = showtimes.get(index);
            PriceResolutionResult result = results.get(index);
            if (!result.isComplete()) {
                continue;
            }
            result.resolvedPrices().stream()
                    .map(resolved -> newSnapshot(
                            showtime,
                            resolved.seatType(),
                            resolved.price(),
                            result.currency(),
                            PricingSource.POLICY,
                            resolved.policy(),
                            resolved.rule(),
                            result.resolvedAt(),
                            result.timezone()))
                    .forEach(snapshots::add);
        }
        if (!snapshots.isEmpty()) {
            showtimePriceRepository.saveAll(snapshots);
            showtimePriceRepository.flush();
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public void validateCompleteness(Showtime showtime) {
        List<String> requiredIds =
                seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(showtime.getAuditorium().getId());
        List<ShowtimePrice> configured =
                showtimePriceRepository.findByShowtimeIdWithSeatType(showtime.getId());
        Map<String, ShowtimePrice> configuredBySeatType = new LinkedHashMap<>();
        Set<String> currencies = new HashSet<>();

        for (ShowtimePrice snapshot : configured) {
            String seatTypeId = snapshot.getSeatType().getPublicId();
            if (configuredBySeatType.putIfAbsent(seatTypeId, snapshot) != null) {
                throw incomplete("Duplicate snapshot for SeatType " + seatTypeId, requiredIds, configured);
            }
            if (snapshot.getPrice() == null || snapshot.getPrice().signum() <= 0) {
                throw incomplete("Snapshot price must be positive for SeatType " + seatTypeId,
                        requiredIds, configured);
            }
            if (!SUPPORTED_CURRENCY.equals(snapshot.getCurrency())) {
                throw incomplete("Unsupported snapshot currency " + snapshot.getCurrency(), requiredIds, configured);
            }
            currencies.add(snapshot.getCurrency());
            if (snapshot.getPricingSource() != null
                    && snapshot.getPricingSource() != PricingSource.LEGACY
                    && (snapshot.getSeatTypeNameSnapshot() == null
                        || snapshot.getSeatTypeCodeSnapshot() == null
                        || snapshot.getResolvedAt() == null
                        || snapshot.getResolutionTimezone() == null)) {
                throw incomplete("Snapshot provenance metadata is incomplete for SeatType " + seatTypeId,
                        requiredIds, configured);
            }
        }

        Set<String> expected = new HashSet<>(requiredIds);
        if (!configuredBySeatType.keySet().equals(expected) || currencies.size() != 1) {
            PriceResolutionResult diagnostics = pricePolicyResolver.resolve(showtime);
            if (diagnostics != null) {
                throw incompleteWithResolution(
                        "Showtime snapshot does not exactly cover required SeatTypes",
                        requiredIds,
                        configured,
                        diagnostics);
            }
            throw incomplete("Showtime snapshot does not exactly cover required SeatTypes", requiredIds, configured);
        }
    }

    private ShowtimePricesResponse buildResponse(Showtime showtime,
                                                  List<ShowtimePrice> prices,
                                                  PriceResolutionResult resolution) {
        List<ShowtimePriceDto> lines = prices.stream().map(this::toDto).toList();
        List<String> requiredIds =
                seatRepository.findActiveSeatTypePublicIdsByAuditoriumId(showtime.getAuditorium().getId());
        Set<String> configuredIds = prices.stream()
                .map(price -> price.getSeatType().getPublicId())
                .collect(Collectors.toSet());

        List<PriceSeatTypeDiagnosticDto> missing;
        List<PriceSeatTypeDiagnosticDto> ambiguous;
        if (resolution == null) {
            Map<String, SeatType> seatTypes = seatTypeRepository
                    .findAllByPublicIdInAndDeletedAtIsNull(requiredIds)
                    .stream()
                    .collect(Collectors.toMap(SeatType::getPublicId, seatType -> seatType));
            missing = requiredIds.stream()
                    .filter(id -> !configuredIds.contains(id))
                    .map(id -> {
                        SeatType seatType = seatTypes.get(id);
                        return new PriceSeatTypeDiagnosticDto(
                                id,
                                seatType == null ? null : seatType.getCode().name(),
                                seatType == null ? null : seatType.getName(),
                                List.of());
                    })
                    .toList();
            ambiguous = List.of();
        } else {
            missing = resolution.missingSeatTypes().stream().map(this::toDiagnostic).toList();
            ambiguous = resolution.ambiguousSeatTypes().stream().map(this::toDiagnostic).toList();
        }

        ShowtimePricesResponse response =
                new ShowtimePricesResponse(prices.isEmpty() ? SUPPORTED_CURRENCY : prices.get(0).getCurrency(), lines);
        response.setMissingSeatTypes(missing);
        response.setAmbiguousSeatTypes(ambiguous);
        Set<String> currencies = prices.stream()
                .map(ShowtimePrice::getCurrency)
                .collect(Collectors.toSet());
        boolean validRows = prices.size() == configuredIds.size()
                && currencies.size() == 1
                && currencies.contains(SUPPORTED_CURRENCY)
                && prices.stream().allMatch(price ->
                        price.getPrice() != null
                        && price.getPrice().signum() > 0
                        && (price.getPricingSource() == null
                            || price.getPricingSource() == PricingSource.LEGACY
                            || (price.getSeatTypeNameSnapshot() != null
                                && price.getSeatTypeCodeSnapshot() != null
                                && price.getResolvedAt() != null
                                && price.getResolutionTimezone() != null)));
        response.setComplete(
                missing.isEmpty()
                && ambiguous.isEmpty()
                && configuredIds.equals(new HashSet<>(requiredIds))
                && validRows);
        return response;
    }

    private ShowtimePriceDto toDto(ShowtimePrice snapshot) {
        ShowtimePriceDto dto = new ShowtimePriceDto(
                snapshot.getSeatType().getPublicId(),
                snapshot.getSeatTypeNameSnapshot() == null
                        ? snapshot.getSeatType().getName() : snapshot.getSeatTypeNameSnapshot(),
                snapshot.getSeatTypeCodeSnapshot() == null
                        ? snapshot.getSeatType().getCode().name() : snapshot.getSeatTypeCodeSnapshot(),
                snapshot.getPrice());
        PricingSource source = snapshot.getPricingSource() == null ? PricingSource.LEGACY : snapshot.getPricingSource();
        dto.setPricingSource(source.name());
        dto.setResolvedAt(snapshot.getResolvedAt());
        dto.setResolutionTimezone(snapshot.getResolutionTimezone());
        if (snapshot.getSourcePolicy() != null) {
            dto.setSourcePolicyId(snapshot.getSourcePolicy().getPublicId());
            dto.setSourcePolicyName(snapshot.getSourcePolicy().getName());
        }
        if (snapshot.getSourceRule() != null) {
            dto.setSourceRuleId(snapshot.getSourceRule().getPublicId());
        }
        return dto;
    }

    private ShowtimePrice newSnapshot(Showtime showtime,
                                      SeatType seatType,
                                      BigDecimal price,
                                      String currency,
                                      PricingSource source,
                                      com.lorafilm.movie.pricing.domain.entity.PricePolicy policy,
                                      com.lorafilm.movie.pricing.domain.entity.PricePolicyRule rule,
                                      Instant resolvedAt,
                                      String timezone) {
        ShowtimePrice snapshot = new ShowtimePrice();
        snapshot.setShowtime(showtime);
        snapshot.setSeatType(seatType);
        snapshot.setSeatTypeNameSnapshot(seatType.getName());
        snapshot.setSeatTypeCodeSnapshot(seatType.getCode().name());
        snapshot.setPrice(price);
        snapshot.setCurrency(currency);
        snapshot.setPricingSource(source);
        snapshot.setSourcePolicy(policy);
        snapshot.setSourceRule(rule);
        snapshot.setResolvedAt(resolvedAt);
        snapshot.setResolutionTimezone(timezone);
        return snapshot;
    }

    private PriceSeatTypeDiagnosticDto toDiagnostic(
            PriceResolutionResult.SeatTypeDiagnostic diagnostic) {
        return new PriceSeatTypeDiagnosticDto(
                diagnostic.seatTypeId(),
                diagnostic.seatTypeCode(),
                diagnostic.seatTypeName(),
                diagnostic.candidateRuleIds());
    }

    private BusinessException incomplete(String message,
                                         List<String> requiredIds,
                                         List<ShowtimePrice> configured) {
        Set<String> configuredIds = configured.stream()
                .map(price -> price.getSeatType().getPublicId())
                .collect(Collectors.toSet());
        List<String> missing = requiredIds.stream().filter(id -> !configuredIds.contains(id)).toList();
        return new BusinessException(ErrorCode.PRICING_INCOMPLETE, message, Map.of(
                "requiredSeatTypeIds", requiredIds,
                "configuredSeatTypeIds", configuredIds,
                "missingSeatTypeIds", missing));
    }

    private BusinessException incompleteWithResolution(String message,
                                                       List<String> requiredIds,
                                                       List<ShowtimePrice> configured,
                                                       PriceResolutionResult diagnostics) {
        Set<String> configuredIds = configured.stream()
                .map(price -> price.getSeatType().getPublicId())
                .collect(Collectors.toSet());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("requiredSeatTypeIds", requiredIds);
        details.put("configuredSeatTypeIds", configuredIds);
        details.put("missingSeatTypes", diagnostics.missingSeatTypes().stream()
                .map(this::toDiagnostic)
                .toList());
        details.put("ambiguousSeatTypes", diagnostics.ambiguousSeatTypes().stream()
                .map(this::toDiagnostic)
                .toList());
        return new BusinessException(ErrorCode.PRICING_INCOMPLETE, message, details);
    }

    private void requireDraft(Showtime showtime) {
        if (showtime.getStatus() != ShowtimeStatus.DRAFT) {
            throw new BusinessException(ErrorCode.SHOWTIME_PRICE_NOT_EDITABLE,
                    "Showtime pricing is mutable only while the Showtime is DRAFT");
        }
    }

    private String requireTimezone(Showtime showtime) {
        if (showtime.getCinema() == null
                || showtime.getCinema().getTimezone() == null
                || showtime.getCinema().getTimezone().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE,
                    "Cinema timezone is required for snapshot evidence");
        }
        try {
            return java.time.ZoneId.of(showtime.getCinema().getTimezone()).getId();
        } catch (java.time.DateTimeException exception) {
            throw new BusinessException(ErrorCode.INVALID_CINEMA_TIMEZONE,
                    "Invalid cinema timezone: " + showtime.getCinema().getTimezone());
        }
    }
}
