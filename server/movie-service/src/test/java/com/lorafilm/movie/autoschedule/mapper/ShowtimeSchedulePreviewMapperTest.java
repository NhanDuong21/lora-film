package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewItemResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewPageResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShowtimeSchedulePreviewMapperTest {

    private ShowtimeSchedulePreviewMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ShowtimeSchedulePreviewMapper(new ObjectMapper());
    }

    @Test
    void toPageResponse_shouldMapCorrectly_whenNull() {
        ShowtimeSchedulePreviewPageResponse response = mapper.toPageResponse(null, null);
        assertThat(response).isNull();
    }

    @Test
    void toPageResponse_shouldMapCorrectly() {
        ShowtimeSchedulePreview preview = mock(ShowtimeSchedulePreview.class);
        when(preview.getPublicId()).thenReturn("preview-1");
        when(preview.getStrategy()).thenReturn(AutoScheduleStrategy.BALANCED);
        when(preview.getApplyMode()).thenReturn(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        when(preview.getStatus()).thenReturn(SchedulePreviewStatus.PREVIEWED);

        org.springframework.data.domain.Page<ShowtimeSchedulePreviewItem> emptyPage = org.springframework.data.domain.Page.empty();
        ShowtimeSchedulePreviewPageResponse response = mapper.toPageResponse(preview, emptyPage);

        assertThat(response).isNotNull();
        assertThat(response.getPreview().getPreviewPublicId()).isEqualTo("preview-1");
        assertThat(response.getPreview().getStrategy()).isEqualTo(AutoScheduleStrategy.BALANCED);
        assertThat(response.getPreview().getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        assertThat(response.getItems().getContent()).isEmpty();
    }

    @Test
    void toItemResponse_shouldPassScoreBreakdown() {
        ShowtimeSchedulePreviewItem item = mock(ShowtimeSchedulePreviewItem.class);
        when(item.getPublicId()).thenReturn("item-1");
        when(item.getServiceDate()).thenReturn(LocalDate.of(2026, 7, 24));
        when(item.getScore()).thenReturn(new BigDecimal("9.5"));
        when(item.getScoreBreakdown()).thenReturn(java.util.Map.of("baseScore", new BigDecimal("5.0"), "timeBonus", new BigDecimal("4.5")));

        ShowtimeSchedulePreviewItemResponse response = mapper.toItemResponse(item);

        assertThat(response).isNotNull();
        assertThat(response.getItemPublicId()).isEqualTo("item-1");
        assertThat(response.getServiceDate()).isEqualTo(LocalDate.of(2026, 7, 24));
        assertThat(response.getScore()).isEqualTo(new BigDecimal("9.5"));
        assertThat(response.getScoreBreakdown()).containsEntry("baseScore", new BigDecimal("5.0"))
                                                .containsEntry("timeBonus", new BigDecimal("4.5"));
    }

    @Test
    void toSummaryResponse_shouldMapCorrectly_whenNull() {
        ShowtimeSchedulePreviewSummaryResponse summary = mapper.toSummaryResponse(null);
        assertThat(summary).isNull();
    }

    @Test
    void toSummaryResponse_shouldMapCorrectly() {
        ShowtimeSchedulePreview preview = mock(ShowtimeSchedulePreview.class);
        Cinema cinema = mock(Cinema.class);
        when(cinema.getPublicId()).thenReturn("cinema-1");
        when(cinema.getSlug()).thenReturn("lora-cinema");
        when(cinema.getName()).thenReturn("Lora Cinema");
        when(preview.getCinema()).thenReturn(cinema);
        when(preview.getTotalCandidateCount()).thenReturn(10);
        when(preview.getValidCandidateCount()).thenReturn(8);
        when(preview.getRejectedCandidateCount()).thenReturn(2);
        when(preview.getSelectedCandidateCount()).thenReturn(6);

        ShowtimeSchedulePreviewSummaryResponse summary = mapper.toSummaryResponse(preview);

        assertThat(summary).isNotNull();
        assertThat(summary.getCinemaPublicId()).isEqualTo("cinema-1");
        assertThat(summary.getCinemaSlug()).isEqualTo("lora-cinema");
        assertThat(summary.getCinemaName()).isEqualTo("Lora Cinema");
        assertThat(summary.getTotalCandidateCount()).isEqualTo(10);
        assertThat(summary.getValidCandidateCount()).isEqualTo(8);
        assertThat(summary.getRejectedCandidateCount()).isEqualTo(2);
        assertThat(summary.getSelectedCandidateCount()).isEqualTo(6);
    }
}
