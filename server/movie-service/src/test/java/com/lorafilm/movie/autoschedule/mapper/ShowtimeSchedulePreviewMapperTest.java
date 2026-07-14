package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewItemResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
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
    void toResponse_shouldMapCorrectly_whenNull() {
        ShowtimeSchedulePreviewResponse response = mapper.toResponse(null, Collections.emptyList());
        assertThat(response).isNull();
    }

    @Test
    void toResponse_shouldMapCorrectly() {
        ShowtimeSchedulePreview preview = mock(ShowtimeSchedulePreview.class);
        when(preview.getPublicId()).thenReturn("preview-1");
        when(preview.getStrategy()).thenReturn(AutoScheduleStrategy.BALANCED);
        when(preview.getApplyMode()).thenReturn(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        when(preview.getStatus()).thenReturn(SchedulePreviewStatus.PREVIEWED);

        ShowtimeSchedulePreviewResponse response = mapper.toResponse(preview, Collections.emptyList());

        assertThat(response).isNotNull();
        assertThat(response.getPreviewPublicId()).isEqualTo("preview-1");
        assertThat(response.getStrategy()).isEqualTo(AutoScheduleStrategy.BALANCED);
        assertThat(response.getStatus()).isEqualTo(SchedulePreviewStatus.PREVIEWED);
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void toItemResponse_shouldParseScoreBreakdown_whenValidJson() {
        ShowtimeSchedulePreviewItem item = mock(ShowtimeSchedulePreviewItem.class);
        when(item.getPublicId()).thenReturn("item-1");
        when(item.getScore()).thenReturn(new BigDecimal("9.5"));
        when(item.getScoreBreakdownJson()).thenReturn("{\"baseScore\": 5.0, \"timeBonus\": 4.5}");

        ShowtimeSchedulePreviewItemResponse response = mapper.toItemResponse(item);

        assertThat(response).isNotNull();
        assertThat(response.getItemPublicId()).isEqualTo("item-1");
        assertThat(response.getScore()).isEqualTo(new BigDecimal("9.5"));
        assertThat(response.getScoreBreakdown()).containsEntry("baseScore", new BigDecimal("5.0"))
                                                .containsEntry("timeBonus", new BigDecimal("4.5"));
    }

    @Test
    void toItemResponse_shouldReturnEmptyMap_whenMalformedJson() {
        ShowtimeSchedulePreviewItem item = mock(ShowtimeSchedulePreviewItem.class);
        when(item.getPublicId()).thenReturn("item-2");
        when(item.getScoreBreakdownJson()).thenReturn("{malformed: true");

        ShowtimeSchedulePreviewItemResponse response = mapper.toItemResponse(item);

        assertThat(response).isNotNull();
        assertThat(response.getScoreBreakdown()).isEmpty();
    }

    @Test
    void toSummary_shouldMapCorrectly() {
        ShowtimeSchedulePreview preview = mock(ShowtimeSchedulePreview.class);
        when(preview.getTotalCandidateCount()).thenReturn(10);
        when(preview.getValidCandidateCount()).thenReturn(8);
        when(preview.getRejectedCandidateCount()).thenReturn(2);
        when(preview.getSelectedCandidateCount()).thenReturn(6);

        ShowtimeSchedulePreviewSummaryResponse summary = mapper.toSummary(preview);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalCandidateCount()).isEqualTo(10);
        assertThat(summary.getValidCandidateCount()).isEqualTo(8);
        assertThat(summary.getRejectedCandidateCount()).isEqualTo(2);
        assertThat(summary.getSelectedCandidateCount()).isEqualTo(6);
    }
}
