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

    public record Blocker(
            String code,
            String message,
            String actionPath,
            List<BlockerDetail> details) {

        public Blocker(String code, String message, String actionPath) {
            this(code, message, actionPath, List.of());
        }
    }

    public record BlockerDetail(
            String code,
            LocalDate serviceDate,
            String auditoriumPublicId,
            String auditoriumName,
            int affectedCandidateCount,
            String message,
            String actionPath) {}
}
