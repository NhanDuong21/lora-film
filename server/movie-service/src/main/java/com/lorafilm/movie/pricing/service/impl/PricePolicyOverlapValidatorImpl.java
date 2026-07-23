package com.lorafilm.movie.pricing.service.impl;

import com.lorafilm.movie.pricing.domain.entity.PricePolicy;
import com.lorafilm.movie.pricing.domain.entity.PricePolicyRule;
import com.lorafilm.movie.pricing.domain.enums.PriceDayType;
import com.lorafilm.movie.pricing.service.PricePolicyOverlapValidator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PricePolicyOverlapValidatorImpl implements PricePolicyOverlapValidator {

    @Override
    public List<Conflict> findConflicts(PricePolicy candidate, List<PricePolicy> activePolicies) {
        List<PricePolicy> comparisonPolicies = new ArrayList<>(activePolicies);
        if (comparisonPolicies.stream().noneMatch(policy -> policy == candidate)) {
            comparisonPolicies.add(candidate);
        }

        List<Conflict> conflicts = new ArrayList<>();
        List<PricePolicyRule> candidateRules = activeRules(candidate);
        for (int i = 0; i < candidateRules.size(); i++) {
            PricePolicyRule first = candidateRules.get(i);
            for (PricePolicy policy : comparisonPolicies) {
                if (policy == candidate
                        || (policy.getId() != null && policy.getId().equals(candidate.getId()))) {
                    continue;
                }
                if (!dateRangesOverlap(candidate, policy)
                        || !candidate.getPriority().equals(policy.getPriority())) {
                    continue;
                }
                for (PricePolicyRule second : activeRules(policy)) {
                    if (rulesConflict(first, second)) {
                        conflicts.add(conflict(first, second));
                    }
                }
            }
            for (int j = i + 1; j < candidateRules.size(); j++) {
                PricePolicyRule second = candidateRules.get(j);
                if (rulesConflict(first, second)) {
                    conflicts.add(conflict(first, second));
                }
            }
        }
        return conflicts.stream()
                .sorted(Comparator.comparing(Conflict::firstRuleId)
                        .thenComparing(Conflict::secondRuleId))
                .distinct()
                .toList();
    }

    private boolean rulesConflict(PricePolicyRule first, PricePolicyRule second) {
        return first.getSeatType().getId().equals(second.getSeatType().getId())
                && sameScopeRankAndCoverage(first, second)
                && sameDayRankAndCoverage(first.getDayType(), second.getDayType())
                && sameTimeRankAndCoverage(first, second);
    }

    private boolean sameScopeRankAndCoverage(PricePolicyRule first, PricePolicyRule second) {
        if ((first.getAuditorium() != null) != (second.getAuditorium() != null)
                || (first.getScreenType() != null) != (second.getScreenType() != null)) {
            return false;
        }
        if (first.getAuditorium() != null) {
            return first.getAuditorium().getId().equals(second.getAuditorium().getId());
        }
        if (first.getScreenType() != null) {
            return first.getScreenType() == second.getScreenType();
        }
        return true;
    }

    private boolean sameDayRankAndCoverage(PriceDayType first, PriceDayType second) {
        if (first.rank() != second.rank()) {
            return false;
        }
        return first == PriceDayType.ALL_DAYS || first == second;
    }

    private boolean sameTimeRankAndCoverage(PricePolicyRule first, PricePolicyRule second) {
        if (first.isBoundedTimeBand() != second.isBoundedTimeBand()) {
            return false;
        }
        if (!first.isBoundedTimeBand()) {
            return true;
        }
        for (TimeInterval left : split(first.getTimeBandStart(), first.getTimeBandEnd())) {
            for (TimeInterval right : split(second.getTimeBandStart(), second.getTimeBandEnd())) {
                if (left.start() < right.end() && right.start() < left.end()) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<TimeInterval> split(LocalTime start, LocalTime end) {
        int startSecond = start.toSecondOfDay();
        int endSecond = end.toSecondOfDay();
        if (startSecond < endSecond) {
            return List.of(new TimeInterval(startSecond, endSecond));
        }
        return List.of(new TimeInterval(startSecond, 86_400), new TimeInterval(0, endSecond));
    }

    private boolean dateRangesOverlap(PricePolicy first, PricePolicy second) {
        LocalDate firstEnd = first.getEffectiveTo() == null ? LocalDate.MAX : first.getEffectiveTo();
        LocalDate secondEnd = second.getEffectiveTo() == null ? LocalDate.MAX : second.getEffectiveTo();
        return !first.getEffectiveFrom().isAfter(secondEnd)
                && !second.getEffectiveFrom().isAfter(firstEnd);
    }

    private List<PricePolicyRule> activeRules(PricePolicy policy) {
        return policy.getRules().stream()
                .filter(PricePolicyRule::isActive)
                .filter(rule -> rule.getDeletedAt() == null)
                .toList();
    }

    private Conflict conflict(PricePolicyRule first, PricePolicyRule second) {
        String firstId = first.getPublicId();
        String secondId = second.getPublicId();
        if (firstId.compareTo(secondId) > 0) {
            String swap = firstId;
            firstId = secondId;
            secondId = swap;
        }
        return new Conflict(firstId, secondId,
                "Rules can produce an equal-rank price for the same Showtime and SeatType");
    }

    private record TimeInterval(int start, int end) {
    }
}
