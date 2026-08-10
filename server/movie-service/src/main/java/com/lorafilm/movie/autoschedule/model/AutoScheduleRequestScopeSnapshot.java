package com.lorafilm.movie.autoschedule.model;

import java.time.LocalDate;
import java.util.List;

public record AutoScheduleRequestScopeSnapshot(
        String cinemaPublicId,
        LocalDate scheduleFrom,
        LocalDate scheduleTo,
        int slotGranularityMinutes,
        List<String> movieVersionPublicIds,
        List<String> auditoriumPublicIds) {
}
