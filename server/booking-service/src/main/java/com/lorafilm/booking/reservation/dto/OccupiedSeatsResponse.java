package com.lorafilm.booking.reservation.dto;

import java.time.Instant;
import java.util.List;

public class OccupiedSeatsResponse {
    private String showtimeIdentifier;
    private List<OccupiedSeatDto> occupiedSeats;
    private int totalOccupied;

    public OccupiedSeatsResponse() {
    }

    public OccupiedSeatsResponse(String showtimeIdentifier, List<OccupiedSeatDto> occupiedSeats) {
        this.showtimeIdentifier = showtimeIdentifier;
        this.occupiedSeats = occupiedSeats;
        this.totalOccupied = occupiedSeats != null ? occupiedSeats.size() : 0;
    }

    public String getShowtimeIdentifier() {
        return showtimeIdentifier;
    }

    public void setShowtimeIdentifier(String showtimeIdentifier) {
        this.showtimeIdentifier = showtimeIdentifier;
    }

    public List<OccupiedSeatDto> getOccupiedSeats() {
        return occupiedSeats;
    }

    public void setOccupiedSeats(List<OccupiedSeatDto> occupiedSeats) {
        this.occupiedSeats = occupiedSeats;
        this.totalOccupied = occupiedSeats != null ? occupiedSeats.size() : 0;
    }

    public int getTotalOccupied() {
        return totalOccupied;
    }

    public void setTotalOccupied(int totalOccupied) {
        this.totalOccupied = totalOccupied;
    }

    public static class OccupiedSeatDto {
        private Long seatId;
        private String seatLabel;
        private String status; // "HELD" or "BOOKED"
        private Instant expiresAt; // null if BOOKED

        public OccupiedSeatDto() {
        }

        public OccupiedSeatDto(Long seatId, String seatLabel, String status, Instant expiresAt) {
            this.seatId = seatId;
            this.seatLabel = seatLabel;
            this.status = status;
            this.expiresAt = expiresAt;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
