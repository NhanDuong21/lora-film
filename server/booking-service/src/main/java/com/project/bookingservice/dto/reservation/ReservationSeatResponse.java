package com.project.bookingservice.dto.reservation;

public class ReservationSeatResponse {
    private Long reservationId;
    private Long seatId;

    public ReservationSeatResponse() {}

    public ReservationSeatResponse(Long reservationId, Long seatId) {
        this.reservationId = reservationId;
        this.seatId = seatId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }
}
