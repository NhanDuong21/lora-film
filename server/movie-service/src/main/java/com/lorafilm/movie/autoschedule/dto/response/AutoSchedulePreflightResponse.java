package com.lorafilm.movie.autoschedule.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AutoSchedulePreflightResponse(
        boolean canGenerate,
        LocalDate planningFrom,
        LocalDate planningTo,
        String timezone,
        int eligibleMovieCount,
        int eligibleVersionCount,
        int eligibleAuditoriumCount,
        int compatiblePairCount,
        List<Blocker> blockers,
        String eligibilityFingerprint,
        String pricingFingerprint,
        String configurationFingerprint,
        List<String> eligibleMovieVersionPublicIds,
        List<String> eligibleAuditoriumPublicIds) {

    public record Blocker(String code, String message, String actionPath) {}
}
