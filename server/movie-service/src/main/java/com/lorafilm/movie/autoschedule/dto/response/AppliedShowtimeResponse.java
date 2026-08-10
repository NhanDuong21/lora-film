package com.lorafilm.movie.autoschedule.dto.response;

import java.time.Instant;

public class AppliedShowtimeResponse {

    private String previewItemPublicId;
    private String showtimePublicId;

    private String moviePublicId;
    private String movieTitle;
    private String movieVersionPublicId;

    private String auditoriumPublicId;
    private String auditoriumName;

    private Instant startTime;
    private Instant endTime;

    private String status;

    public String getPreviewItemPublicId() {
        return previewItemPublicId;
    }

    public void setPreviewItemPublicId(String previewItemPublicId) {
        this.previewItemPublicId = previewItemPublicId;
    }

    public String getShowtimePublicId() {
        return showtimePublicId;
    }

    public void setShowtimePublicId(String showtimePublicId) {
        this.showtimePublicId = showtimePublicId;
    }

    public String getMoviePublicId() {
        return moviePublicId;
    }

    public void setMoviePublicId(String moviePublicId) {
        this.moviePublicId = moviePublicId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getMovieVersionPublicId() {
        return movieVersionPublicId;
    }

    public void setMovieVersionPublicId(String movieVersionPublicId) {
        this.movieVersionPublicId = movieVersionPublicId;
    }

    public String getAuditoriumPublicId() {
        return auditoriumPublicId;
    }

    public void setAuditoriumPublicId(String auditoriumPublicId) {
        this.auditoriumPublicId = auditoriumPublicId;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
