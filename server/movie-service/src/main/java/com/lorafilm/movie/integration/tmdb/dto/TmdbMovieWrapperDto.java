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
    private java.util.List<TmdbGenreDto> genres;
    private TmdbCreditsDto credits;
    private TmdbVideosDto videos;
    private TmdbMediaDto media;
    private java.util.List<TmdbProductionCompanyDto> productionCompanies;
    private java.util.List<TmdbTranslationDto> translations;

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

    public java.util.List<TmdbGenreDto> getGenres() {
        return genres;
    }

    public void setGenres(java.util.List<TmdbGenreDto> genres) {
        this.genres = genres;
    }

    public TmdbCreditsDto getCredits() {
        return credits;
    }

    public void setCredits(TmdbCreditsDto credits) {
        this.credits = credits;
    }

    public TmdbVideosDto getVideos() {
        return videos;
    }

    public void setVideos(TmdbVideosDto videos) {
        this.videos = videos;
    }

    public TmdbMediaDto getMedia() {
        return media;
    }

    public void setMedia(TmdbMediaDto media) {
        this.media = media;
    }

    public java.util.List<TmdbProductionCompanyDto> getProductionCompanies() {
        return productionCompanies;
    }

    public void setProductionCompanies(java.util.List<TmdbProductionCompanyDto> productionCompanies) {
        this.productionCompanies = productionCompanies;
    }

    public java.util.List<TmdbTranslationDto> getTranslations() {
        return translations;
    }

    public void setTranslations(java.util.List<TmdbTranslationDto> translations) {
        this.translations = translations;
    }
}
