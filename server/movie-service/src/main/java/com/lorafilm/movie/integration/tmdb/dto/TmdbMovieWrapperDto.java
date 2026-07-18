package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieWrapperDto {
    private Long tmdbId;
    private LocalDateTime lastUpdated;
    private Integer qualityScore;
    private String qualityStatus;
    private TmdbMovieDetailsDto movie;

    public Long getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(Long tmdbId) {
        this.tmdbId = tmdbId;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public TmdbMovieDetailsDto getMovie() {
        return movie;
    }

    public void setMovie(TmdbMovieDetailsDto movie) {
        this.movie = movie;
    }
}
