package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.showtime.domain.entity.Showtime;

import java.util.List;

public class CandidateScoringContext {
    private final Cinema cinema;
    private final List<OperatingWindow> operatingWindows;
    private final List<Showtime> existingShowtimes;

    public CandidateScoringContext(Cinema cinema, List<OperatingWindow> operatingWindows, List<Showtime> existingShowtimes) {
        this.cinema = cinema;
        this.operatingWindows = operatingWindows;
        this.existingShowtimes = existingShowtimes;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public List<OperatingWindow> getOperatingWindows() {
        return operatingWindows;
    }

    public List<Showtime> getExistingShowtimes() {
        return existingShowtimes;
    }
}
