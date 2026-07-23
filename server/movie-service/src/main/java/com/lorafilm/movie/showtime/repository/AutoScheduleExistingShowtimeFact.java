package com.lorafilm.movie.showtime.repository;

import java.time.Instant;

/** Lightweight cinema-wide fact used only by auto-schedule fairness generation. */
public interface AutoScheduleExistingShowtimeFact {

    Long getMovieId();

    String getMoviePublicId();

    Instant getStartTime();
}
