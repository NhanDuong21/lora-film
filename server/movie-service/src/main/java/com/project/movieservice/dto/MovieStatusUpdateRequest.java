package com.project.movieservice.dto;

import jakarta.validation.constraints.NotBlank;

public class MovieStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
