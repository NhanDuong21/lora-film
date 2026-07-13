package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;

public class GenreRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private ActiveStatus status;

    public GenreRequest() {}
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
