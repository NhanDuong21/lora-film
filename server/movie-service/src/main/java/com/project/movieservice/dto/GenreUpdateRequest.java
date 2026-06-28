package com.project.movieservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GenreUpdateRequest {

    @NotBlank(message = "Genre name is required")
    @Size(max = 100, message = "Genre name must not exceed 100 characters")
    @Pattern(regexp = "^(?=.*[a-zA-ZÀ-ỹ])[a-zA-ZÀ-ỹ0-9\\s\\-&]+$", message = "Genre name must contain at least one letter and only allow letters, numbers, spaces, hyphens or ampersands")
    private String genreName;

    public GenreUpdateRequest() {
    }

    public GenreUpdateRequest(String genreName) {
        this.genreName = genreName;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }
}
