package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.model.OperatingWindow;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

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

    private ShowtimeCandidate candidate() {
        ShowtimeCandidate candidate = new ShowtimeCandidate();
        candidate.setStartTime(Instant.parse("2026-07-24T13:00:00Z"));
        candidate.setEndTime(Instant.parse("2026-07-24T14:00:00Z"));
        candidate.setOccupancyEndTime(Instant.parse("2026-07-24T14:15:00Z"));
        return candidate;
    }
}
