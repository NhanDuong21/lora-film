package com.lorafilm.movie.autoschedule.service.impl;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.autoschedule.repository.ShowtimeSchedulePreviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShowtimeSchedulePreviewExpiryService {

    private final ShowtimeSchedulePreviewRepository repository;

    public ShowtimeSchedulePreviewExpiryService(ShowtimeSchedulePreviewRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireIfNecessary(String previewPublicId, Instant now) {
        ShowtimeSchedulePreview preview = repository.findByPublicId(previewPublicId)
                .orElseThrow(() -> new com.lorafilm.movie.common.exception.BusinessException(com.lorafilm.movie.common.exception.ErrorCode.AUTO_SCHEDULE_PREVIEW_NOT_FOUND));
        
        if (preview.getStatus() == SchedulePreviewStatus.PREVIEWED && !now.isBefore(preview.getExpiresAt())) {
            preview.setStatus(SchedulePreviewStatus.EXPIRED);
            repository.saveAndFlush(preview);
            return true;
        }
        return preview.getStatus() == SchedulePreviewStatus.EXPIRED;
    }
}
