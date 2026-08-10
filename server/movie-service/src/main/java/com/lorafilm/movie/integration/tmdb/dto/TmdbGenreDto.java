package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbGenreDto {
    private Long tmdbGenreId;
    private String name;

    public Long getTmdbGenreId() {
        return tmdbGenreId;
    }

    public void setTmdbGenreId(Long tmdbGenreId) {
        this.tmdbGenreId = tmdbGenreId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
