package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateMovieMediaRequest {

    @NotNull(message = "Media type is required")
    private MovieMediaType mediaType;

    @NotBlank(message = "URL is required")
    @Size(max = 500, message = "URL cannot exceed 500 characters")
    private String url;

    @Size(max = 150, message = "Title cannot exceed 150 characters")
    private String title;

    @NotNull(message = "Display order is required")
    private Integer displayOrder;

    @NotNull(message = "isPrimary is required")
    private Boolean isPrimary;

    @NotNull(message = "Status is required")
    private ActiveStatus status;

    public UpdateMovieMediaRequest() {}

    public UpdateMovieMediaRequest(MovieMediaType mediaType, String url, String title, Integer displayOrder, Boolean isPrimary, ActiveStatus status) {
        this.mediaType = mediaType;
        this.url = url;
        this.title = title;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
        this.status = status;
    }

    public MovieMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MovieMediaType mediaType) {
        this.mediaType = mediaType;
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
