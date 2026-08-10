package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreflightRequest;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.autoschedule.model.AutoSchedulePreflightResult;

public interface AutoSchedulePreflightService {
    AutoSchedulePreflightResult prepare(AutoSchedulePreflightRequest request);

    default AutoSchedulePreflightResponse preflight(AutoSchedulePreflightRequest request) {
        return prepare(request).response();
    }
}
