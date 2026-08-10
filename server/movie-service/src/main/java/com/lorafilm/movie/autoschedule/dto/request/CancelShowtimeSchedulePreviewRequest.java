package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class CancelShowtimeSchedulePreviewRequest {

    @NotNull
    @PositiveOrZero
    private Long expectedVersion;

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }
}
