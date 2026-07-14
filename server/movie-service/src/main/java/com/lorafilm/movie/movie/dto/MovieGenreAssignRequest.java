package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MovieGenreAssignRequest {
    @NotNull(message = "Genre IDs cannot be null")
    private List<String> genreIds;

    public MovieGenreAssignRequest() {}

    public List<String> getGenreIds() { return genreIds; }
    public void setGenreIds(List<String> genreIds) { this.genreIds = genreIds; }
}
