package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMediaDto {
    private TmdbImageDto primaryPoster;
    private TmdbImageDto primaryBackdrop;
    private List<TmdbImageDto> posters;
    private List<TmdbImageDto> backdrops;

    public TmdbImageDto getPrimaryPoster() {
        return primaryPoster;
    }

    public void setPrimaryPoster(TmdbImageDto primaryPoster) {
        this.primaryPoster = primaryPoster;
    }

    public TmdbImageDto getPrimaryBackdrop() {
        return primaryBackdrop;
    }

    public void setPrimaryBackdrop(TmdbImageDto primaryBackdrop) {
        this.primaryBackdrop = primaryBackdrop;
    }

    public List<TmdbImageDto> getPosters() {
        return posters;
    }

    public void setPosters(List<TmdbImageDto> posters) {
        this.posters = posters;
    }

    public List<TmdbImageDto> getBackdrops() {
        return backdrops;
    }

    public void setBackdrops(List<TmdbImageDto> backdrops) {
        this.backdrops = backdrops;
    }
}
