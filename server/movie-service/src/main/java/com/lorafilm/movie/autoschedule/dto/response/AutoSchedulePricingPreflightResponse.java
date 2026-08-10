package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.pricing.dto.response.PriceSeatTypeDiagnosticDto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AutoSchedulePricingPreflightResponse(
        boolean complete,
        int totalCandidateCount,
        int completeCandidateCount,
        int incompleteCandidateCount,
        int ambiguousCandidateCount,
        List<ReasonGroup> reasonGroups,
        List<CandidateResult> candidates
) {
    public record ReasonGroup(
            String reasonCode,
            String displayMessage,
            int count,
            List<LocalDate> affectedDates,
            List<AuditoriumRef> auditoriums,
            List<SeatTypeRef> seatTypes
    ) {
    }

    public record CandidateResult(
            String previewItemPublicId,
            boolean complete,
            String reasonCode,
            String displayMessage,
            LocalDate serviceDate,
            Instant startTime,
            String auditoriumPublicId,
            String auditoriumName,
            List<PriceSeatTypeDiagnosticDto> missingSeatTypes,
            List<PriceSeatTypeDiagnosticDto> ambiguousSeatTypes
    ) {
    }

    public record AuditoriumRef(String publicId, String name) {
    }

    public record SeatTypeRef(String publicId, String code, String name) {
    }
}
