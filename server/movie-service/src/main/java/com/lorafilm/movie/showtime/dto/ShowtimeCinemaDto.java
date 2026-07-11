package com.lorafilm.movie.showtime.dto;

public class ShowtimeCinemaDto {
    private String publicId;
    private String slug;
    private String name;
    private String timezone;

    public ShowtimeCinemaDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
