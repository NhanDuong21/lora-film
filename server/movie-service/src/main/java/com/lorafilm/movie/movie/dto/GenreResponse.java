package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;

public class GenreResponse {
    private String publicId;
    private String name;
    private String slug;
    private ActiveStatus status;

    public GenreResponse() {}
    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
