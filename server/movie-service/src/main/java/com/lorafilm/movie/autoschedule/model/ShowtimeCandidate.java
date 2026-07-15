package com.lorafilm.movie.autoschedule.model;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ShowtimeCandidate {
    private Movie movie;
    private MovieVersion movieVersion;
    private Cinema cinema;
    private Auditorium auditorium;

    private Instant startTime;
    private Instant endTime;
    private Instant occupancyEndTime;

    private PreviewItemValidationStatus validationStatus;
    private String rejectionCode;
    private String rejectionReason;

    private BigDecimal score = BigDecimal.ZERO;
    private Map<String, BigDecimal> scoreBreakdown = new HashMap<>();

    private Integer rankingPosition;
    private boolean selected;

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public MovieVersion getMovieVersion() {
        return movieVersion;
    }

    public void setMovieVersion(MovieVersion movieVersion) {
        this.movieVersion = movieVersion;
    }

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public Auditorium getAuditorium() {
        return auditorium;
    }

    public void setAuditorium(Auditorium auditorium) {
        this.auditorium = auditorium;
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

    public Instant getOccupancyEndTime() {
        return occupancyEndTime;
    }

    public void setOccupancyEndTime(Instant occupancyEndTime) {
        this.occupancyEndTime = occupancyEndTime;
    }

    public PreviewItemValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(PreviewItemValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getRejectionCode() {
        return rejectionCode;
    }

    public void setRejectionCode(String rejectionCode) {
        this.rejectionCode = rejectionCode;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Map<String, BigDecimal> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(Map<String, BigDecimal> scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public Integer getRankingPosition() {
        return rankingPosition;
    }

    public void setRankingPosition(Integer rankingPosition) {
        this.rankingPosition = rankingPosition;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
