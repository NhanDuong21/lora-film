package com.lorafilm.movie.movie.dto;

public record TmdbQueueBreakdownResponse(
        long total,
        long future,
        long old,
        long undated) {
}
