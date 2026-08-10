package com.lorafilm.movie.movie.dto;

public record TmdbQueueBreakdownResponse(
        long total,
        long eligibleUpcoming,
        long releaseDateExpired,
        long undated) {
}
