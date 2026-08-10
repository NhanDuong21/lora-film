package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.impl.AutoSchedulePreviewPricingReadinessServiceImpl;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoSchedulePreviewPricingReadinessServiceTest {

    @Mock
    private ShowtimeSchedulePreviewRepository previewRepository;
    @Mock
    private ShowtimeSchedulePreviewItemRepository itemRepository;
    @Mock
    private AutoSchedulePricingPreflightService pricingPreflightService;

    private AutoSchedulePreviewPricingReadinessService service;
    private ShowtimeSchedulePreview preview;

    @BeforeEach
    void setUp() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-08-05T02:00:00Z"), ZoneOffset.UTC);
        service = new AutoSchedulePreviewPricingReadinessServiceImpl(
                previewRepository, itemRepository, pricingPreflightService, clock);

        var constructor = ShowtimeSchedulePreview.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        preview = constructor.newInstance();
        ReflectionTestUtils.setField(preview, "id", 10L);
        preview.setPublicId("preview-1");
        preview.setVersion(3L);
        preview.setStatus(SchedulePreviewStatus.PREVIEWED);
        preview.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview.setSelectedCandidateCount(1);
        preview.setExpiresAt(Instant.parse("2026-08-05T03:00:00Z"));
    }

    @Test
    void check_returnsGroupedPricingReadinessWithoutApplying() {
        ShowtimeSchedulePreviewItem item = new ShowtimeSchedulePreviewItem();
        AutoSchedulePricingPreflightResponse response = new AutoSchedulePricingPreflightResponse(
                false, 1, 0, 1, 0,
                List.of(new AutoSchedulePricingPreflightResponse.ReasonGroup(
                        "PRICING_INCOMPLETE", "Thiếu bảng giá.", 1,
                        List.of(), List.of(), List.of())),
                List.of());
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemsForApply(10L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of(item));
        when(pricingPreflightService.evaluate(List.of(item)))
                .thenReturn(new AutoSchedulePricingPreflightService.Evaluation(response, List.of()));

        AutoSchedulePricingPreflightResponse result = service.check("preview-1", 3L);

        assertThat(result).isSameAs(response);
        assertThat(result.complete()).isFalse();
        verify(pricingPreflightService).evaluate(List.of(item));
    }

    @Test
    void check_rejectsStalePreviewVersion() {
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));

        assertThatThrownBy(() -> service.check("preview-1", 2L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
    }

    @Test
    void check_rejectsPreviewWithoutSelectedCandidates() {
        preview.setSelectedCandidateCount(0);
        when(previewRepository.findByPublicIdWithCinema("preview-1")).thenReturn(Optional.of(preview));
        when(itemRepository.findSelectedItemsForApply(10L, PreviewItemValidationStatus.VALID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.check("preview-1", 3L))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getErrorCode())
                .isEqualTo(ErrorCode.AUTO_SCHEDULE_NO_SELECTED_ITEMS);
    }
}
