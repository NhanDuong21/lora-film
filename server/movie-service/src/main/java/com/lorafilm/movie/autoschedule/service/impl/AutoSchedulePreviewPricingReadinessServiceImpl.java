package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePricingPreflightResponse;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewItemRepository;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePricingPreflightService;
import com.lorafilm.movie.autoschedule.service.AutoSchedulePreviewPricingReadinessService;
import com.lorafilm.movie.common.exception.BusinessException;
import com.lorafilm.movie.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class AutoSchedulePreviewPricingReadinessServiceImpl
        implements AutoSchedulePreviewPricingReadinessService {

    private final ShowtimeSchedulePreviewRepository previewRepository;
    private final ShowtimeSchedulePreviewItemRepository itemRepository;
    private final AutoSchedulePricingPreflightService pricingPreflightService;
    private final Clock clock;

    public AutoSchedulePreviewPricingReadinessServiceImpl(
            ShowtimeSchedulePreviewRepository previewRepository,
            ShowtimeSchedulePreviewItemRepository itemRepository,
            AutoSchedulePricingPreflightService pricingPreflightService,
            Clock clock) {
        this.previewRepository = previewRepository;
        this.itemRepository = itemRepository;
        this.pricingPreflightService = pricingPreflightService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public AutoSchedulePricingPreflightResponse check(String previewPublicId, Long expectedVersion) {
        ShowtimeSchedulePreview preview = previewRepository.findByPublicIdWithCinema(previewPublicId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));

        if (!Objects.equals(preview.getVersion(), expectedVersion)) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_VERSION_CONFLICT);
        }
        if (preview.getStatus() != SchedulePreviewStatus.PREVIEWED
                || preview.getApplyMode() != SchedulePreviewApplyMode.ALL_OR_NOTHING) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_APPLICABLE);
        }
        if (!Instant.now(clock).isBefore(preview.getExpiresAt())) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_EXPIRED);
        }

        List<ShowtimeSchedulePreviewItem> selectedItems = itemRepository.findSelectedItemsForApply(
                preview.getId(), PreviewItemValidationStatus.VALID);
        if (selectedItems.isEmpty() || preview.getSelectedCandidateCount() == 0) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_NO_SELECTED_ITEMS);
        }
        if (selectedItems.size() != preview.getSelectedCandidateCount()) {
            throw new BusinessException(ErrorCode.AUTO_SCHEDULE_PREVIEW_DATA_INCONSISTENT);
        }

        return pricingPreflightService.evaluate(selectedItems).response();
    }
}
