package com.lorafilm.booking.reservation.mapper;

import com.lorafilm.booking.reservation.dto.SeatReservationResponse;
import com.lorafilm.booking.reservation.entity.SeatReservation;
import org.springframework.stereotype.Component;

@Component
public class SeatReservationMapper {

    public SeatReservationResponse toResponse(SeatReservation reservation) {
        if (reservation == null) {
            return null;
        }

        SeatReservationResponse response = new SeatReservationResponse();
        response.setId(reservation.getId());
        response.setPublicId(reservation.getPublicId());
        response.setReservationCode(reservation.getReservationCode());
        response.setUserId(reservation.getUserId());
        response.setShowtimeId(reservation.getShowtimeId());
        response.setSeatId(reservation.getSeatId());
        response.setSeatLabel(reservation.getSeatLabel());
        response.setSeatType(reservation.getSeatType());
        response.setStatus(reservation.getStatus());
        response.setExpiresAt(reservation.getExpiresAt());
        response.setReservedAt(reservation.getReservedAt());
        response.setBookingId(reservation.getBookingId());
        return response;
    }
}
