package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateCinemaMediaRequest {

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL cannot exceed 500 characters")
    private String url;

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    private Integer displayOrder;
    private Boolean isPrimary;

    @NotNull(message = "Status is required")
    private ActiveStatus status;

    public UpdateCinemaMediaRequest() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }
}
