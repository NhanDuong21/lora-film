package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;

import java.util.List;

public class CandidateGenerationContext {
    private final NormalizedGeneratePreviewRequest request;
    private final Cinema cinema;
    private final List<Auditorium> auditoriums;
    private final List<MovieVersion> movieVersions;

    public CandidateGenerationContext(NormalizedGeneratePreviewRequest request, Cinema cinema,
                                      List<Auditorium> auditoriums, List<MovieVersion> movieVersions) {
        this.request = request;
        this.cinema = cinema;
        this.auditoriums = auditoriums;
        this.movieVersions = movieVersions;
    }

    public NormalizedGeneratePreviewRequest getRequest() {
        return request;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public List<Auditorium> getAuditoriums() {
        return auditoriums;
    }

    public List<MovieVersion> getMovieVersions() {
        return movieVersions;
    }
}
