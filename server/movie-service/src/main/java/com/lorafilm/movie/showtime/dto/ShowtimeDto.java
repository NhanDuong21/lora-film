package com.lorafilm.movie.showtime.dto;

import java.time.Instant;

public class ShowtimeDto {
    private String publicId;
    private String movieSlug;
    private String movieTitle;
    private String cinemaSlug;
    private String cinemaName;
    private String auditoriumName;
    private String movieVersionName;
    private Instant startTime;
    private Instant endTime;
    private String status;

    public ShowtimeDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getMovieSlug() { return movieSlug; }
    public void setMovieSlug(String movieSlug) { this.movieSlug = movieSlug; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public String getCinemaSlug() { return cinemaSlug; }
    public void setCinemaSlug(String cinemaSlug) { this.cinemaSlug = cinemaSlug; }
    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }
    public String getAuditoriumName() { return auditoriumName; }
    public void setAuditoriumName(String auditoriumName) { this.auditoriumName = auditoriumName; }
    public String getMovieVersionName() { return movieVersionName; }
    public void setMovieVersionName(String movieVersionName) { this.movieVersionName = movieVersionName; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
