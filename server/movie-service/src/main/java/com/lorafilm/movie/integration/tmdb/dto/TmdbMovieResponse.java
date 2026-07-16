package com.lorafilm.movie.integration.tmdb.dto;

import java.util.List;

public class TmdbMovieResponse {
    private String nextCursor;
    private List<TmdbMovieDto> movies;

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public List<TmdbMovieDto> getMovies() {
        return movies;
    }

    public void setMovies(List<TmdbMovieDto> movies) {
        this.movies = movies;
    }
}
