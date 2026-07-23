package com.lorafilm.movie.pricing.service;

import com.lorafilm.movie.pricing.domain.entity.PricePolicy;

import java.util.List;

public interface PricePolicyOverlapValidator {
    List<Conflict> findConflicts(PricePolicy candidate, List<PricePolicy> activePolicies);

    record Conflict(String firstRuleId, String secondRuleId, String message) {
    }
}
