package com.lorafilm.movie.showtime.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class UpdateShowtimeRequest {

    @NotBlank(message = "Movie public ID is required")
    private String moviePublicId;

    @NotBlank(message = "Movie version public ID is required")
    private String movieVersionPublicId;

    @NotBlank(message = "Cinema public ID is required")
    private String cinemaPublicId;

    @NotBlank(message = "Auditorium public ID is required")
    private String auditoriumPublicId;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    public UpdateShowtimeRequest() {}

    public String getMoviePublicId() {
        return moviePublicId;
    }

    public void setMoviePublicId(String moviePublicId) {
        this.moviePublicId = moviePublicId;
    }

    public String getMovieVersionPublicId() {
        return movieVersionPublicId;
    }

    public void setMovieVersionPublicId(String movieVersionPublicId) {
        this.movieVersionPublicId = movieVersionPublicId;
    }

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public void setCinemaPublicId(String cinemaPublicId) {
        this.cinemaPublicId = cinemaPublicId;
    }

    public String getAuditoriumPublicId() {
        return auditoriumPublicId;
    }

    public void setAuditoriumPublicId(String auditoriumPublicId) {
        this.auditoriumPublicId = auditoriumPublicId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
}
