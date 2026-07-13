package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GenreRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name must not exceed 50 characters")
    @Pattern(regexp = "^(?=.*[a-zA-Z\\p{L}])[a-zA-Z0-9\\p{L}\\s\\-]+$", message = "Name must contain at least one letter and cannot contain special characters")
    private String name;
    private ActiveStatus status;

    public GenreRequest() {}
    public String getName() { return name; }
    public void setName(String name) {
        if (name == null) {
            this.name = null;
            return;
        }
        String trimmed = name.trim();
        if (!trimmed.isEmpty()) {
            this.name = trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1);
        } else {
            this.name = trimmed;
        }
    }
    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
