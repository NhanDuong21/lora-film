package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;

import java.time.LocalTime;
import java.util.List;

public interface PricePolicyOverlapValidator {
    List<Conflict> findConflicts(PricePolicy candidate, List<PricePolicy> activePolicies);

    record Conflict(
            String reasonCode,
            String firstRuleId,
            String secondRuleId,
            String seatTypeId,
            String seatTypeCode,
            String seatTypeName,
            String scope,
            String auditoriumId,
            String auditoriumName,
            String screenType,
            PriceDayType dayType,
            LocalTime timeBandStart,
            LocalTime timeBandEnd,
            int conflictingRuleCount,
            String message
    ) {
        public Conflict(String firstRuleId, String secondRuleId, String message) {
            this("PRICE_POLICY_OVERLAP", firstRuleId, secondRuleId,
                    null, null, null, null, null, null, null,
                    null, null, null, 2, message);
        }
    }
}
