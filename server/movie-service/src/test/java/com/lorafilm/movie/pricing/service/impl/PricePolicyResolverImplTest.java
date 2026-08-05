package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import com.lorafilm.movie.pricing.domain.enums.PricePolicyStatus;
import com.lorafilm.movie.pricing.repository.PricePolicyRepository;
import com.lorafilm.movie.pricing.service.model.PriceResolutionResult;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import com.lorafilm.movie.seat.domain.enums.SeatTypeCode;
import com.lorafilm.movie.seat.repository.SeatRepository;
import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PricePolicyResolverImplTest {

    @Mock
    private PricePolicyRepository policyRepository;
    @Mock
    private SeatRepository seatRepository;

    private PricePolicyResolverImpl resolver;
    private Showtime showtime;
    private SeatType vip;
    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        resolver = new PricePolicyResolverImpl(
                policyRepository,
                seatRepository,
                Clock.fixed(Instant.parse("2026-07-23T00:00:00Z"), ZoneOffset.UTC));
        Cinema cinema = new Cinema();
        cinema.setId(1L);
        cinema.setPublicId("cinema-1");
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        auditorium = new Auditorium();
        auditorium.setId(2L);
        auditorium.setPublicId("auditorium-1");
        auditorium.setCinema(cinema);
        auditorium.setScreenType(ScreenType.IMAX);
        vip = new SeatType();
        vip.setId(3L);
        vip.setPublicId("vip-1");
        vip.setCode(SeatTypeCode.VIP);
        vip.setName("VIP");
        showtime = new Showtime();
        showtime.setCinema(cinema);
        showtime.setAuditorium(auditorium);
        showtime.setStartTime(Instant.parse("2026-07-25T03:00:00Z")); // Saturday 10:00 local
        lenient().when(seatRepository.findActiveSeatTypesByAuditoriumId(2L)).thenReturn(List.of(vip));
    }

    @Test
    void auditoriumScopeWinsBeforeHigherPolicyPriority() {
        PricePolicy broad = policy("broad", 100, rule("broad-rule", null, null,
                PriceDayType.ALL_DAYS, null, null, "200000"));
        PricePolicy auditoriumPolicy = policy("auditorium", 0, rule("auditorium-rule", auditorium, null,
                PriceDayType.ALL_DAYS, null, null, "120000"));
        when(policyRepository.findEffectiveActivePolicies(1L, LocalDate.of(2026, 7, 25)))
                .thenReturn(List.of(broad, auditoriumPolicy));

        PriceResolutionResult result = resolver.resolve(showtime);

        assertTrue(result.isComplete());
        assertEquals(new BigDecimal("120000"), result.resolvedPrices().getFirst().price());
        assertEquals("auditorium-rule", result.resolvedPrices().getFirst().rule().getPublicId());
    }

    @Test
    void resolutionPreviewPrecedenceIsAuditoriumThenScreenThenCinema() {
        PricePolicy policy = policy(
                "scope-precedence",
                0,
                rule("cinema-rule", null, null,
                        PriceDayType.ALL_DAYS, null, null, "75000"),
                rule("screen-rule", null, ScreenType.IMAX,
                        PriceDayType.ALL_DAYS, null, null, "90000"),
                rule("auditorium-rule", auditorium, null,
                        PriceDayType.ALL_DAYS, null, null, "120000"));
        when(policyRepository.findEffectiveActivePolicies(1L, LocalDate.of(2026, 7, 25)))
                .thenReturn(List.of(policy));

        PriceResolutionResult result = resolver.resolve(showtime);

        assertTrue(result.isComplete());
        assertEquals("auditorium-rule", result.resolvedPrices().getFirst().rule().getPublicId());
        assertEquals(new BigDecimal("120000"), result.resolvedPrices().getFirst().price());
    }

    @Test
    void equalHighestRankIsReportedAsAmbiguous() {
        PricePolicy first = policy("first", 10, rule("rule-a", null, null,
                PriceDayType.WEEKEND, LocalTime.of(9, 0), LocalTime.of(12, 0), "100000"));
        PricePolicy second = policy("second", 10, rule("rule-b", null, null,
                PriceDayType.WEEKEND, LocalTime.of(9, 30), LocalTime.of(11, 0), "110000"));
        when(policyRepository.findEffectiveActivePolicies(1L, LocalDate.of(2026, 7, 25)))
                .thenReturn(List.of(first, second));

        PriceResolutionResult result = resolver.resolve(showtime);

        assertFalse(result.isComplete());
        assertEquals(List.of("rule-a", "rule-b"),
                result.ambiguousSeatTypes().getFirst().candidateRuleIds());
        assertTrue(result.resolvedPrices().isEmpty());
    }

    @Test
    void overnightBandMatchesAfterMidnightUsingStartDateDayType() {
        showtime.setStartTime(Instant.parse("2026-07-24T18:00:00Z")); // Saturday 01:00 local
        PricePolicy policy = policy("overnight", 0, rule("overnight-rule", null, null,
                PriceDayType.WEEKEND, LocalTime.of(22, 0), LocalTime.of(2, 0), "90000"));
        when(policyRepository.findEffectiveActivePolicies(1L, LocalDate.of(2026, 7, 25)))
                .thenReturn(List.of(policy));

        PriceResolutionResult result = resolver.resolve(showtime);

        assertTrue(result.isComplete());
        assertEquals(new BigDecimal("90000"), result.resolvedPrices().getFirst().price());
    }

    @Test
    void invalidCinemaTimezoneFailsWithoutUtcFallback() {
        showtime.getCinema().setTimezone("Invalid/Timezone");

        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(showtime));

        assertEquals(ErrorCode.INVALID_CINEMA_TIMEZONE, exception.getErrorCode());
    }

    @Test
    void accessibleSeatUsesStandardPriceWhenNoDedicatedRuleExists() {
        SeatType standard = new SeatType();
        standard.setId(4L);
        standard.setPublicId("standard-1");
        standard.setCode(SeatTypeCode.STANDARD);
        standard.setName("Standard");
        SeatType accessible = new SeatType();
        accessible.setId(5L);
        accessible.setPublicId("accessible-1");
        accessible.setCode(SeatTypeCode.DISABLED);
        accessible.setName("Ghế hỗ trợ");
        PricePolicyRule standardRule = rule("standard-rule", null, null,
                PriceDayType.ALL_DAYS, null, null, "75000");
        standardRule.setSeatType(standard);
        PricePolicy policy = policy("accessible-default", 0, standardRule);
        when(seatRepository.findActiveSeatTypesByAuditoriumId(2L))
                .thenReturn(List.of(standard, accessible));
        when(policyRepository.findEffectiveActivePolicies(1L, LocalDate.of(2026, 7, 25)))
                .thenReturn(List.of(policy));

        PriceResolutionResult result = resolver.resolve(showtime);

        assertTrue(result.isComplete());
        assertEquals(2, result.resolvedPrices().size());
        assertEquals(new BigDecimal("75000"), result.resolvedPrices().get(1).price());
        assertEquals(SeatTypeCode.DISABLED, result.resolvedPrices().get(1).seatType().getCode());
    }

    @Test
    void bulkResolutionLoadsOnePolicyRangePerCinemaAndFiltersByLocalDate() {
        Showtime nextDay = new Showtime();
        nextDay.setCinema(showtime.getCinema());
        nextDay.setAuditorium(auditorium);
        nextDay.setStartTime(Instant.parse("2026-07-26T03:00:00Z"));
        PricePolicy firstDay = policy("first-day", 0, rule("first-day-rule", null, null,
                PriceDayType.ALL_DAYS, null, null, "100000"));
        firstDay.setEffectiveFrom(LocalDate.of(2026, 7, 25));
        firstDay.setEffectiveTo(LocalDate.of(2026, 7, 25));
        PricePolicy secondDay = policy("second-day", 0, rule("second-day-rule", null, null,
                PriceDayType.ALL_DAYS, null, null, "110000"));
        secondDay.setEffectiveFrom(LocalDate.of(2026, 7, 26));
        when(policyRepository.findActivePoliciesOverlappingDateRange(
                1L, LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 26)))
                .thenReturn(List.of(firstDay, secondDay));

        List<PriceResolutionResult> results = resolver.resolveAll(List.of(showtime, nextDay));

        assertEquals(new BigDecimal("100000"), results.get(0).resolvedPrices().getFirst().price());
        assertEquals(new BigDecimal("110000"), results.get(1).resolvedPrices().getFirst().price());
        verify(policyRepository).findActivePoliciesOverlappingDateRange(
                1L, LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 26));
    }

    private PricePolicy policy(String publicId, int priority, PricePolicyRule... rules) {
        PricePolicy policy = new PricePolicy();
        policy.setPublicId(publicId);
        policy.setCinema(showtime.getCinema());
        policy.setStatus(PricePolicyStatus.ACTIVE);
        policy.setCurrency("VND");
        policy.setPriority(priority);
        policy.setEffectiveFrom(LocalDate.of(2020, 1, 1));
        for (PricePolicyRule rule : rules) {
            policy.addRule(rule);
        }
        return policy;
    }

    private PricePolicyRule rule(String publicId,
                                 Auditorium scopedAuditorium,
                                 ScreenType screenType,
                                 PriceDayType dayType,
                                 LocalTime start,
                                 LocalTime end,
                                 String price) {
        PricePolicyRule rule = new PricePolicyRule();
        rule.setPublicId(publicId);
        rule.setSeatType(vip);
        rule.setAuditorium(scopedAuditorium);
        rule.setScreenType(screenType);
        rule.setDayType(dayType);
        rule.setTimeBandStart(start);
        rule.setTimeBandEnd(end);
        rule.setPrice(new BigDecimal(price));
        rule.setActive(true);
        return rule;
    }
}
