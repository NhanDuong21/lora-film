package com.lorafilm.booking.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;

public class CreateTicketRequest {

    @NotNull(message = "Seat ID is required")
    private Long seatId;

    @NotNull(message = "Seat label is required")
    private String seatLabel;

    private String seatRow;
    private Integer seatColumn;
    private String seatType;

    @NotNull(message = "Ticket price is required")
    @Positive(message = "Ticket price must be positive")
    private BigDecimal ticketPrice;

    private String movieTitle;
    private String cinemaName;
    private String auditoriumName;
    private Instant showtimeStart;
    private Instant showtimeEnd;
    private String movieFormat;
    private String audioLanguage;
    private String subtitleLanguage;

    public CreateTicketRequest() {
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatColumn() {
        return seatColumn;
    }

    public void setSeatColumn(Integer seatColumn) {
        this.seatColumn = seatColumn;
    }

    public String getSeatType() {
        return seatType;
    }

    public void setSeatType(String seatType) {
        this.seatType = seatType;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getAuditoriumName() {
        return auditoriumName;
    }

    public void setAuditoriumName(String auditoriumName) {
        this.auditoriumName = auditoriumName;
    }

    public Instant getShowtimeStart() {
        return showtimeStart;
    }

    public void setShowtimeStart(Instant showtimeStart) {
        this.showtimeStart = showtimeStart;
    }

    public Instant getShowtimeEnd() {
        return showtimeEnd;
    }

    public void setShowtimeEnd(Instant showtimeEnd) {
        this.showtimeEnd = showtimeEnd;
    }

    public String getMovieFormat() {
        return movieFormat;
    }

    public void setMovieFormat(String movieFormat) {
        this.movieFormat = movieFormat;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }
}
