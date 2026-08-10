package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.dto.response.AutoSchedulePreflightResponse;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;

import java.util.List;
import java.util.Set;

public record AutoSchedulePreflightResult(
        AutoSchedulePreflightResponse response,
        Cinema cinema,
        List<Auditorium> auditoriums,
        List<MovieVersion> movieVersions,
        Set<CompatiblePair> compatiblePairs) {

    public record CompatiblePair(Long movieVersionId, Long auditoriumId) {}
}
