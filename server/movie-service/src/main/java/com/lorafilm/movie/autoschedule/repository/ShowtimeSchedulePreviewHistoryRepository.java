package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.dto.request.AutoSchedulePreviewHistoryQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShowtimeSchedulePreviewHistoryRepository {

    Page<ShowtimeSchedulePreviewHistoryRow> findHistory(
            AutoSchedulePreviewHistoryQuery query,
            Pageable pageable
    );
}
