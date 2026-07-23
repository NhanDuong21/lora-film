package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;

import java.time.Instant;

/** Scalar state used to validate manual selection without loading preview item entities. */
public record SelectionItemSnapshot(
        Long itemId,
        String itemPublicId,
        Long previewId,
        Long auditoriumId,
        Instant startTime,
        Instant endTime,
        Instant occupancyEndTime,
        PreviewItemValidationStatus validationStatus,
        Boolean selected,
        PreviewItemApplyStatus applyStatus) {
}
