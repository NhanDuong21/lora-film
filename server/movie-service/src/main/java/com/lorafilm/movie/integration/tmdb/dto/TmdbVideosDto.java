package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbVideosDto {
    private TmdbTrailerDto primaryTrailer;

    public TmdbTrailerDto getPrimaryTrailer() {
        return primaryTrailer;
    }

    public void setPrimaryTrailer(TmdbTrailerDto primaryTrailer) {
        this.primaryTrailer = primaryTrailer;
    }
}
