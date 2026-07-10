package com.lorafilm.movie.showtime.dto;

import java.time.Instant;

public class ShowtimeDto {
    private String showtimePublicId;
    private ShowtimeMovieDto movie;
    private ShowtimeMovieVersionDto movieVersion;
    private ShowtimeCinemaDto cinema;
    private ShowtimeAuditoriumDto auditorium;
    private Instant startTime;
    private Instant endTime;
    private String status;

    public ShowtimeDto() {}

    public String getShowtimePublicId() { return showtimePublicId; }
    public void setShowtimePublicId(String showtimePublicId) { this.showtimePublicId = showtimePublicId; }
    public ShowtimeMovieDto getMovie() { return movie; }
    public void setMovie(ShowtimeMovieDto movie) { this.movie = movie; }
    public ShowtimeMovieVersionDto getMovieVersion() { return movieVersion; }
    public void setMovieVersion(ShowtimeMovieVersionDto movieVersion) { this.movieVersion = movieVersion; }
    public ShowtimeCinemaDto getCinema() { return cinema; }
    public void setCinema(ShowtimeCinemaDto cinema) { this.cinema = cinema; }
    public ShowtimeAuditoriumDto getAuditorium() { return auditorium; }
    public void setAuditorium(ShowtimeAuditoriumDto auditorium) { this.auditorium = auditorium; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
