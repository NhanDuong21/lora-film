package com.lorafilm.movie.showtime.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ShowtimeDto {
    private String showtimePublicId;
    private ShowtimeMovieDto movie;
    private ShowtimeMovieVersionDto movieVersion;
    private ShowtimeCinemaDto cinema;
    private ShowtimeAuditoriumDto auditorium;
    private Instant startTime;
    private Instant endTime;
    private LocalDate serviceDate;
    private LocalDateTime localStartTime;
    private LocalDateTime localEndTime;
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
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public LocalDateTime getLocalStartTime() { return localStartTime; }
    public void setLocalStartTime(LocalDateTime localStartTime) { this.localStartTime = localStartTime; }
    public LocalDateTime getLocalEndTime() { return localEndTime; }
    public void setLocalEndTime(LocalDateTime localEndTime) { this.localEndTime = localEndTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
