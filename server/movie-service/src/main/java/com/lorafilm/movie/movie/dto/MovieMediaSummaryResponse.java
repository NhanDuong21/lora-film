package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;

public class MovieMediaSummaryResponse {

    private String publicId;
    private MovieMediaType mediaType;
    private String url;
    private String title;
    private Integer displayOrder;
    private Boolean isPrimary;
    private ActiveStatus status;

    public MovieMediaSummaryResponse() {}

    public MovieMediaSummaryResponse(String publicId, MovieMediaType mediaType, String url, String title, Integer displayOrder, Boolean isPrimary, ActiveStatus status) {
        this.publicId = publicId;
        this.mediaType = mediaType;
        this.url = url;
        this.title = title;
        this.displayOrder = displayOrder;
        this.isPrimary = isPrimary;
        this.status = status;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
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
