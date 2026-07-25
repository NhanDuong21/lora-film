package com.lorafilm.booking.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShowtimeSeatLayoutResponse {
    private Long showtimeId;
    private String showtimePublicId;
    private Instant startTime;
    private Instant endTime;
    private String status; // E.g., "ACTIVE", "CANCELLED"
    private Long auditoriumId;
    private List<SeatDetailDto> seats;

    public ShowtimeSeatLayoutResponse() {
    }

    public String getShowtimePublicId() {
        return showtimePublicId;
    }

    public void setShowtimePublicId(String showtimePublicId) {
        this.showtimePublicId = showtimePublicId;
    }

    public ShowtimeSeatLayoutResponse(Long showtimeId, Instant startTime, Instant endTime, String status, Long auditoriumId, List<SeatDetailDto> seats) {
        this.showtimeId = showtimeId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.auditoriumId = auditoriumId;
        this.seats = seats;
    }

    public Long getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(Long showtimeId) {
        this.showtimeId = showtimeId;
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

    public Long getAuditoriumId() {
        return auditoriumId;
    }

    public void setAuditoriumId(Long auditoriumId) {
        this.auditoriumId = auditoriumId;
    }

    public List<SeatDetailDto> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatDetailDto> seats) {
        this.seats = seats;
    }

    public static class SeatDetailDto {
        @JsonProperty("id")
        private Long seatId;
        private String seatCode; // E.g., "A1", "B5"
        private String seatType; // E.g., "STANDARD", "VIP", "COUPLE"
        private Long pairedSeatId; // For couple seats
        @JsonProperty("blockedForShowtime")
        private boolean isBlocked; // Blocked for showtime or inactive
        @JsonProperty("positionRow")
        private int rowIndex;
        @JsonProperty("positionColumn")
        private int columnIndex;

        public SeatDetailDto() {
        }

        public SeatDetailDto(Long seatId, String seatCode, String seatType, Long pairedSeatId, boolean isBlocked, int rowIndex, int columnIndex) {
            this.seatId = seatId;
            this.seatCode = seatCode;
            this.seatType = seatType;
            this.pairedSeatId = pairedSeatId;
            this.isBlocked = isBlocked;
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
        }

        public Long getSeatId() {
            return seatId;
        }

        public void setSeatId(Long seatId) {
            this.seatId = seatId;
        }

        public String getSeatCode() {
            return seatCode;
        }

        public void setSeatCode(String seatCode) {
            this.seatCode = seatCode;
        }

        public String getSeatType() {
            return seatType;
        }

        public void setSeatType(String seatType) {
            this.seatType = seatType;
        }

        public Long getPairedSeatId() {
            return pairedSeatId;
        }

        public void setPairedSeatId(Long pairedSeatId) {
            this.pairedSeatId = pairedSeatId;
        }

        public boolean isBlocked() {
            return isBlocked;
        }

        public void setBlocked(boolean blocked) {
            isBlocked = blocked;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public void setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }

        public int getColumnIndex() {
            return columnIndex;
        }

        public void setColumnIndex(int columnIndex) {
            this.columnIndex = columnIndex;
        }
    }
}
