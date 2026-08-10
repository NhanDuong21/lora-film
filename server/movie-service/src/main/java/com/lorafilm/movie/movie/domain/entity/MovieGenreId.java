package com.lorafilm.movie.movie.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class MovieGenreId implements java.io.Serializable {

    private Long movie;

    private Long genre;

    public MovieGenreId() {}

    public Long getMovie() {
        return movie;
    }

    public void setMovie(Long movie) {
        this.movie = movie;
    }

    public Long getGenre() {
        return genre;
    }

    public void setGenre(Long genre) {
        this.genre = genre;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieGenreId that = (MovieGenreId) o;
        return Objects.equals(movie, that.movie) && Objects.equals(genre, that.genre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(movie, genre);
    }
}
