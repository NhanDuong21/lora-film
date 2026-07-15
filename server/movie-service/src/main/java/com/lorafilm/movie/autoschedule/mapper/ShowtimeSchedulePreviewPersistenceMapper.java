package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.model.ShowtimeCandidate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShowtimeSchedulePreviewPersistenceMapper {

    private final ObjectMapper objectMapper;

    public ShowtimeSchedulePreviewPersistenceMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ShowtimeSchedulePreviewItem toEntity(ShowtimeCandidate candidate, ShowtimeSchedulePreview preview) {
        return ShowtimeSchedulePreviewItem.createItem(preview, candidate);
    }
}
