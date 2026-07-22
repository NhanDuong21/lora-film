package com.lorafilm.movie.autoschedule.service;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.model.AutoScheduleGenerationContext;
import com.lorafilm.movie.autoschedule.model.NormalizedGeneratePreviewRequest;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;

import java.util.List;

public interface AutoScheduleGenerationContextLoader {
    AutoScheduleGenerationContext load(NormalizedGeneratePreviewRequest request,
                                       Cinema cinema,
                                       List<Auditorium> auditoriums,
                                       List<MovieVersion> movieVersions);
}
