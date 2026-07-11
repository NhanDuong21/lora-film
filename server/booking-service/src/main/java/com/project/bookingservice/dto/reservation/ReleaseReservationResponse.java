package com.project.bookingservice.dto.reservation;

public class ReleaseReservationResponse {

    private Long reservationId;
    private String status;

    public ReleaseReservationResponse() {
    }

    public ReleaseReservationResponse(Long reservationId, String status) {
        this.reservationId = reservationId;
        this.status = status;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
