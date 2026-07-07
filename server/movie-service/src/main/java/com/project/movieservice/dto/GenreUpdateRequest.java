package com.project.movieservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GenreUpdateRequest {

    @NotBlank(message = "Genre name is required")
    @Size(max = 100, message = "Genre name must not exceed 100 characters")
    @Pattern(regexp = "^(?=.*[a-zA-ZÀ-ỹ])[a-zA-ZÀ-ỹ0-9\\s\\-&]+$", message = "Genre name must contain at least one letter and can only include letters, numbers, spaces, hyphens, and ampersands")
    private String genreName;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    private String status;

    public GenreUpdateRequest() {
    }

    public GenreUpdateRequest(String genreName, String status) {
        this.genreName = genreName;
        this.status = status;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
