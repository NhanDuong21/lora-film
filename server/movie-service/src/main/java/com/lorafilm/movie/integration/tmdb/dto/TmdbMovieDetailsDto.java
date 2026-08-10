package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDetailsDto {
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterPath;
    private String backdropPath;
    private String releaseDate;
    private Integer runtimeMinutes;
    private Double voteAverage;
    private Integer voteCount;
    private Double popularity;
    private Boolean adult;
    private String originalLanguage;
    private List<TmdbCountryDto> countries;
    
    // Getters and Setters
    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public Integer getRuntimeMinutes() { return runtimeMinutes; }
    public void setRuntimeMinutes(Integer runtimeMinutes) { this.runtimeMinutes = runtimeMinutes; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    public Integer getVoteCount() { return voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    public Double getPopularity() { return popularity; }
    public void setPopularity(Double popularity) { this.popularity = popularity; }
    public Boolean getAdult() { return adult; }
    public void setAdult(Boolean adult) { this.adult = adult; }
    public String getOriginalLanguage() { return originalLanguage; }
    public void setOriginalLanguage(String originalLanguage) { this.originalLanguage = originalLanguage; }
    public List<TmdbCountryDto> getCountries() { return countries; }
    public void setCountries(List<TmdbCountryDto> countries) { this.countries = countries; }
}
