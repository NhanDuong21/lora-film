package com.lorafilm.booking.reservation.dto;

import java.time.Instant;

public class ExtendReservationResponse {
    private String publicId;
    private String reservationCode;
    private Instant newExpiresAt;
    private long extendedSeconds;

    public ExtendReservationResponse() {
    }

    public ExtendReservationResponse(String publicId, String reservationCode, Instant newExpiresAt, long extendedSeconds) {
        this.publicId = publicId;
        this.reservationCode = reservationCode;
        this.newExpiresAt = newExpiresAt;
        this.extendedSeconds = extendedSeconds;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public Instant getNewExpiresAt() {
        return newExpiresAt;
    }

    public void setNewExpiresAt(Instant newExpiresAt) {
        this.newExpiresAt = newExpiresAt;
    }

    public long getExtendedSeconds() {
        return extendedSeconds;
    }

    public void setExtendedSeconds(long extendedSeconds) {
        this.extendedSeconds = extendedSeconds;
    }
}
