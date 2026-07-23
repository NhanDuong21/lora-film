package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import com.lorafilm.movie.seat.domain.entity.SeatType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricePolicyOverlapValidatorImplTest {

    private final PricePolicyOverlapValidatorImpl validator = new PricePolicyOverlapValidatorImpl();
    private SeatType seatType;
    private Auditorium auditorium;

    @BeforeEach
    void setUp() {
        seatType = new SeatType();
        seatType.setId(1L);
        auditorium = new Auditorium();
        auditorium.setId(2L);
    }

    @Test
    void broadBaseAndAuditoriumOverrideAreAllowed() {
        PricePolicy candidate = policy("candidate", 0,
                rule("candidate-rule", auditorium, PriceDayType.ALL_DAYS, null, null));
        PricePolicy active = policy("active", 0,
                rule("active-rule", null, PriceDayType.ALL_DAYS, null, null));

        assertTrue(validator.findConflicts(candidate, List.of(active)).isEmpty());
    }

    @Test
    void intersectingEqualRankBandsConflict() {
        PricePolicy candidate = policy("candidate", 4,
                rule("rule-b", null, PriceDayType.WEEKDAY,
                        LocalTime.of(10, 0), LocalTime.of(13, 0)));
        PricePolicy active = policy("active", 4,
                rule("rule-a", null, PriceDayType.WEEKDAY,
                        LocalTime.of(12, 0), LocalTime.of(14, 0)));

        var conflicts = validator.findConflicts(candidate, List.of(active));

        assertEquals(1, conflicts.size());
        assertEquals("rule-a", conflicts.getFirst().firstRuleId());
        assertEquals("rule-b", conflicts.getFirst().secondRuleId());
    }

    @Test
    void touchingHalfOpenBandEndpointsDoNotConflict() {
        PricePolicy candidate = policy("candidate", 4,
                rule("rule-b", null, PriceDayType.WEEKEND,
                        LocalTime.of(12, 0), LocalTime.of(14, 0)));
        PricePolicy active = policy("active", 4,
                rule("rule-a", null, PriceDayType.WEEKEND,
                        LocalTime.of(10, 0), LocalTime.NOON));

        assertTrue(validator.findConflicts(candidate, List.of(active)).isEmpty());
    }

    @Test
    void overnightBandsAreSplitForOverlapChecks() {
        PricePolicy candidate = policy("candidate", 1,
                rule("rule-b", null, PriceDayType.ALL_DAYS,
                        LocalTime.of(23, 0), LocalTime.of(2, 0)));
        PricePolicy active = policy("active", 1,
                rule("rule-a", null, PriceDayType.ALL_DAYS,
                        LocalTime.of(1, 0), LocalTime.of(3, 0)));

        assertEquals(1, validator.findConflicts(candidate, List.of(active)).size());
    }

    private PricePolicy policy(String publicId, int priority, PricePolicyRule rule) {
        PricePolicy policy = new PricePolicy();
        policy.setPublicId(publicId);
        policy.setPriority(priority);
        policy.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        policy.setEffectiveTo(LocalDate.of(2026, 12, 31));
        policy.addRule(rule);
        return policy;
    }

    private PricePolicyRule rule(String publicId,
                                 Auditorium scopedAuditorium,
                                 PriceDayType dayType,
                                 LocalTime start,
                                 LocalTime end) {
        PricePolicyRule rule = new PricePolicyRule();
        rule.setPublicId(publicId);
        rule.setSeatType(seatType);
        rule.setAuditorium(scopedAuditorium);
        rule.setDayType(dayType);
        rule.setTimeBandStart(start);
        rule.setTimeBandEnd(end);
        rule.setPrice(new BigDecimal("100000"));
        rule.setActive(true);
        return rule;
    }
}
