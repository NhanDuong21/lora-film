package com.lorafilm.movie.showtime.dto;

public class ShowtimeMovieDto {
    private String publicId;
    private String slug;
    private String title;

    public ShowtimeMovieDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
