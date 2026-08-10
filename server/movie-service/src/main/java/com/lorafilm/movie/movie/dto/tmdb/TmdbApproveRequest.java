package com.lorafilm.movie.movie.dto.tmdb;

import jakarta.validation.constraints.NotNull;

public class TmdbApproveRequest {
    @NotNull(message = "TMDB ID is required")
    private Integer tmdbId;

    public TmdbApproveRequest() {}
    
    public Integer getTmdbId() {
        return tmdbId;
    }
    
    public void setTmdbId(Integer tmdbId) {
        this.tmdbId = tmdbId;
    }
}
