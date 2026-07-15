package com.lorafilm.movie.cinema.dto;

import com.lorafilm.movie.cinema.domain.enums.CinemaMediaType;
import com.lorafilm.movie.common.enums.ActiveStatus;

public class CinemaMediaResponse {

    private String publicId;
    private CinemaMediaType mediaType;
    private String url;
    private String title;
    private Integer displayOrder;
    private Boolean isPrimary;
    private ActiveStatus status;

    public CinemaMediaResponse() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public CinemaMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(CinemaMediaType mediaType) {
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
