package com.lorafilm.movie.pricing.dto.response;

import com.lorafilm.movie.pricing.service.PricePolicyOverlapValidator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PricePolicyResponse(
        String publicId,
        String name,
        String cinemaId,
        String cinemaName,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String storedStatus,
        String displayStatus,
        String currency,
        Integer priority,
        String supersedesPolicyId,
        Instant activatedAt,
        Long activatedBy,
        Instant deactivatedAt,
        Long deactivatedBy,
        String deactivationReason,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<PricePolicyRuleResponse> rules,
        List<PricePolicyOverlapValidator.Conflict> conflicts
) {
}
