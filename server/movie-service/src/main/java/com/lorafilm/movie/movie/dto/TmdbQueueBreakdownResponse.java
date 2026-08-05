package com.lorafilm.movie.movie.dto;

public record TmdbQueueBreakdownResponse(
        long total,
        long future,
        long readyToShow,
        long needsSchedule,
        long undated) {
}
