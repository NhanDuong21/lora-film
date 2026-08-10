package com.lorafilm.movie.movie.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public class TmdbMovieDetailResponse {
    
    private Integer id;
    private String title;
    
    @JsonProperty("original_title")
    private String originalTitle;
    
    private String overview;
    private Integer runtime;
    
    @JsonProperty("release_date")
    private LocalDate releaseDate;
    
    private Boolean adult;
    
    private List<TmdbGenreDto> genres;
    
    @JsonProperty("production_countries")
    private List<TmdbProductionCountryDto> productionCountries;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    
    public Integer getRuntime() { return runtime; }
    public void setRuntime(Integer runtime) { this.runtime = runtime; }
    
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    
    public Boolean getAdult() { return adult; }
    public void setAdult(Boolean adult) { this.adult = adult; }
    
    public List<TmdbGenreDto> getGenres() { return genres; }
    public void setGenres(List<TmdbGenreDto> genres) { this.genres = genres; }
    
    public List<TmdbProductionCountryDto> getProductionCountries() { return productionCountries; }
    public void setProductionCountries(List<TmdbProductionCountryDto> productionCountries) { this.productionCountries = productionCountries; }
}
