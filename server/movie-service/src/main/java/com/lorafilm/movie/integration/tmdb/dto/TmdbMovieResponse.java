package com.lorafilm.movie.integration.tmdb.dto;

import java.util.List;

public class TmdbMovieResponse {
    private String cursor;
    private String nextCursor;
    private Integer limit;
    private Boolean hasMore;
    private List<TmdbMovieWrapperDto> movies;

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }

    public List<TmdbMovieWrapperDto> getMovies() {
        return movies;
    }

    public void setMovies(List<TmdbMovieWrapperDto> movies) {
        this.movies = movies;
    }
}
