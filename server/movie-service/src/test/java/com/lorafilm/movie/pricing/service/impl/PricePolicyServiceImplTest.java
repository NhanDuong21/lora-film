package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.auditorium.repository.AuditoriumRepository;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.common.security.CurrentUserProvider;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import com.lorafilm.movie.pricing.dto.request.ActivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.DeactivatePricePolicyRequest;
import com.lorafilm.movie.pricing.dto.request.UpdatePricePolicyRequest;
import com.lorafilm.movie.pricing.repository.PricePolicyRepository;
import com.lorafilm.movie.pricing.repository.ShowtimePriceRepository;
import com.lorafilm.movie.pricing.service.PricePolicyOverlapValidator;
import com.lorafilm.movie.pricing.service.PricePolicyResolver;
import com.lorafilm.movie.seat.repository.SeatTypeRepository;
import com.lorafilm.movie.showtime.repository.ShowtimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePolicyServiceImplTest {

    @Mock private PricePolicyRepository policyRepository;
    @Mock private CinemaRepository cinemaRepository;
    @Mock private AuditoriumRepository auditoriumRepository;
    @Mock private SeatTypeRepository seatTypeRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private ShowtimePriceRepository showtimePriceRepository;
    @Mock private PricePolicyOverlapValidator overlapValidator;
    @Mock private PricePolicyResolver resolver;
    @Mock private CurrentUserProvider currentUserProvider;

    private PricePolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PricePolicyServiceImpl(
                policyRepository,
                cinemaRepository,
                auditoriumRepository,
                seatTypeRepository,
                showtimeRepository,
                showtimePriceRepository,
                overlapValidator,
                resolver,
                currentUserProvider,
                Clock.fixed(Instant.parse("2026-07-23T03:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void activePolicyCannotBeEdited() {
        PricePolicy policy = policy(PricePolicyStatus.ACTIVE, 4L);
        when(policyRepository.findByPublicIdForUpdate("policy-1")).thenReturn(Optional.of(policy));

        BusinessException error = assertThrows(BusinessException.class, () -> service.update(
                "policy-1",
                new UpdatePricePolicyRequest(4L, "Changed", "cinema-1",
                        LocalDate.of(2026, 7, 1), null, "VND", 0, List.of())));

        assertEquals(ErrorCode.PRICE_POLICY_IMMUTABLE, error.getErrorCode());
    }

    @Test
    void activationRejectsEqualRankConflict() {
        PricePolicy policy = policy(PricePolicyStatus.DRAFT, 1L);
        PricePolicyRule rule = new PricePolicyRule();
        rule.setActive(true);
        policy.addRule(rule);
        when(policyRepository.findByPublicIdForUpdate("policy-1")).thenReturn(Optional.of(policy));
        when(policyRepository.findActivePoliciesOverlappingDateRange(
                10L, LocalDate.of(2026, 1, 1), LocalDate.of(9999, 12, 31)))
                .thenReturn(List.of());
        when(overlapValidator.findConflicts(policy, List.of())).thenReturn(List.of(
                new PricePolicyOverlapValidator.Conflict("rule-a", "rule-b", "equal rank")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.activate("policy-1", new ActivatePricePolicyRequest(1L)));

        assertEquals(ErrorCode.PRICE_POLICY_OVERLAP, error.getErrorCode());
    }

    @Test
    void deactivationRecordsActorTimeAndReason() {
        PricePolicy policy = policy(PricePolicyStatus.ACTIVE, 2L);
        when(policyRepository.findByPublicIdForUpdate("policy-1")).thenReturn(Optional.of(policy));
        when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
        when(policyRepository.saveAndFlush(any(PricePolicy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.deactivate(
                "policy-1", new DeactivatePricePolicyRequest(2L, "Pricing season ended"));

        assertEquals("INACTIVE", response.storedStatus());
        assertEquals(42L, response.deactivatedBy());
        assertEquals(Instant.parse("2026-07-23T03:00:00Z"), response.deactivatedAt());
        assertEquals("Pricing season ended", response.deactivationReason());
    }

    @Test
    void expiredDisplayStatusCanBeFilteredAndPaged() {
        PricePolicy expired = policy(PricePolicyStatus.ACTIVE, 0L);
        expired.setEffectiveTo(LocalDate.of(2026, 7, 22));
        when(policyRepository.findActiveDisplayCandidates(null, null)).thenReturn(List.of(expired));

        var result = service.search(null, "EXPIRED", null, 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("EXPIRED", result.getData().getFirst().displayStatus());
    }

    private PricePolicy policy(PricePolicyStatus status, Long version) {
        Cinema cinema = new Cinema();
        cinema.setId(10L);
        cinema.setPublicId("cinema-1");
        cinema.setName("Cinema One");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        PricePolicy policy = new PricePolicy();
        policy.setPublicId("policy-1");
        policy.setName("Policy One");
        policy.setCinema(cinema);
        policy.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        policy.setCurrency("VND");
        policy.setPriority(0);
        policy.setStatus(status);
        policy.setVersion(version);
        return policy;
    }
}
