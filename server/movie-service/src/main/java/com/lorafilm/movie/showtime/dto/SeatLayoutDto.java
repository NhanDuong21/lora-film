package com.lorafilm.movie.showtime.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class SeatLayoutDto {
    private String showtimePublicId;
    private Long showtimeId;
    private ShowtimeMovieDto movie;
    private ShowtimeMovieVersionDto movieVersion;
    private ShowtimeCinemaDto cinema;
    private ShowtimeAuditoriumDto auditorium;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private List<SeatPriceDto> seats;

    public SeatLayoutDto() {}

    public String getShowtimePublicId() { return showtimePublicId; }
    public void setShowtimePublicId(String showtimePublicId) { this.showtimePublicId = showtimePublicId; }
    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long showtimeId) { this.showtimeId = showtimeId; }
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
    public List<SeatPriceDto> getSeats() { return seats; }
    public void setSeats(List<SeatPriceDto> seats) { this.seats = seats; }

    public static class SeatPriceDto {
        private Long id;
        private String publicId;
        private String seatCode;
        private String rowLabel;
        private Integer seatNumber;
        private Integer positionRow;
        private Integer positionColumn;
        private String seatType;
        private String pairGroup;
        private Long pairedSeatId;
        private BigDecimal price;
        private String currency;
        private String status;
        private boolean blockedForShowtime;

        public SeatPriceDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getPublicId() { return publicId; }
        public void setPublicId(String publicId) { this.publicId = publicId; }
        public String getSeatCode() { return seatCode; }
        public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
        public String getRowLabel() { return rowLabel; }
        public void setRowLabel(String rowLabel) { this.rowLabel = rowLabel; }
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getPositionRow() { return positionRow; }
        public void setPositionRow(Integer positionRow) { this.positionRow = positionRow; }
        public Integer getPositionColumn() { return positionColumn; }
        public void setPositionColumn(Integer positionColumn) { this.positionColumn = positionColumn; }
        public String getSeatType() { return seatType; }
        public void setSeatType(String seatType) { this.seatType = seatType; }
        public String getPairGroup() { return pairGroup; }
        public void setPairGroup(String pairGroup) { this.pairGroup = pairGroup; }
        public Long getPairedSeatId() { return pairedSeatId; }
        public void setPairedSeatId(Long pairedSeatId) { this.pairedSeatId = pairedSeatId; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isBlockedForShowtime() { return blockedForShowtime; }
        public void setBlockedForShowtime(boolean blockedForShowtime) { this.blockedForShowtime = blockedForShowtime; }
    }
}
