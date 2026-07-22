package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShowtimeSchedulePreviewPersistenceMapperTest {

    private final ShowtimeSchedulePreviewPersistenceMapper mapper =
            new ShowtimeSchedulePreviewPersistenceMapper(new ObjectMapper());

    @Test
    void persistsAuthoritativeDateFromOriginatingOperatingWindow() {
        LocalDate serviceDate = LocalDate.of(2026, 7, 24);
        ShowtimeCandidate candidate = candidate();
        candidate.setOperatingWindow(new OperatingWindow(
                serviceDate,
                Instant.parse("2026-07-24T13:00:00Z"),
                Instant.parse("2026-07-24T19:00:00Z")));

        ShowtimeSchedulePreviewItem item = mapper.toEntity(candidate, null);

        assertThat(item.getServiceDate()).isEqualTo(serviceDate);
    }

    @Test
    void rejectsGeneratedCandidateWithoutAuthoritativeServiceDate() {
        ShowtimeCandidate missingWindow = candidate();
        ShowtimeCandidate missingDate = candidate();
        missingDate.setOperatingWindow(new OperatingWindow(
                null,
                Instant.parse("2026-07-24T13:00:00Z"),
                Instant.parse("2026-07-24T19:00:00Z")));

        assertThatThrownBy(() -> mapper.toEntity(missingWindow, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authoritative service date");
        assertThatThrownBy(() -> mapper.toEntity(missingDate, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("authoritative service date");
    }

    @Test
    void preservesS4EffectiveScoreCanonicalBreakdownAndSelection() {
        ShowtimeCandidate candidate = candidate();
        candidate.setOperatingWindow(new OperatingWindow(
                LocalDate.of(2026, 7, 24),
                Instant.parse("2026-07-24T13:00:00Z"),
                Instant.parse("2026-07-24T19:00:00Z")));
        LinkedHashMap<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("base", new BigDecimal("50.000"));
        breakdown.put("primeTime", new BigDecimal("20.000"));
        breakdown.put("offPeak", new BigDecimal("0.000"));
        breakdown.put("earlySlot", new BigDecimal("5.000"));
        breakdown.put("auditoriumFit", new BigDecimal("10.000"));
        breakdown.put("scheduleContinuity", new BigDecimal("0.000"));
        breakdown.put("coverageSearchAdjustment", new BigDecimal("20.000"));
        candidate.setScore(new BigDecimal("105.000"));
        candidate.setScoreBreakdown(breakdown);
        candidate.setRankingPosition(1);
        candidate.setValidationStatus(PreviewItemValidationStatus.VALID);
        candidate.setSelected(true);

        ShowtimeSchedulePreviewItem item = mapper.toEntity(candidate, null);

        assertThat(item.getScore()).isEqualByComparingTo("105.000");
        assertThat(new java.util.ArrayList<>(item.getScoreBreakdown().keySet()))
                .isEqualTo(List.of("base", "primeTime", "offPeak", "earlySlot",
                        "auditoriumFit", "scheduleContinuity", "coverageSearchAdjustment"));
        assertThat(item.getScoreBreakdown().values().stream()
                .reduce(new BigDecimal("0.000"), BigDecimal::add))
                .isEqualByComparingTo(item.getScore());
        assertThat(item.getSelected()).isTrue();
    }

    private ShowtimeCandidate candidate() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setStartTime(Instant.parse("2026-07-24T13:00:00Z"));
        candidate.setEndTime(Instant.parse("2026-07-24T14:00:00Z"));
        candidate.setOccupancyEndTime(Instant.parse("2026-07-24T14:15:00Z"));
        return candidate;
    }
}
