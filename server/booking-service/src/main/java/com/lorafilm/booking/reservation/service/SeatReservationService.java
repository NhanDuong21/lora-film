package com.lorafilm.booking.reservation.service;

import com.lorafilm.booking.reservation.dto.ConvertReservationRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatRequest;
import com.lorafilm.booking.reservation.dto.HoldSeatResponse;
import com.lorafilm.booking.reservation.dto.ReleaseSeatRequest;
import com.lorafilm.booking.reservation.dto.SeatAvailabilityResponse;
import com.lorafilm.booking.reservation.dto.SeatReservationResponse;
import com.lorafilm.booking.reservation.enums.SeatReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SeatReservationService {

    HoldSeatResponse holdSeats(Long userId, HoldSeatRequest request);

    void releaseSeats(Long userId, ReleaseSeatRequest request);

    void releaseSeatsInternal(List<Long> reservationIds, String reason);

    void convertReservations(ConvertReservationRequest request);

    void expireReservations(List<Long> reservationIds);

    SeatReservationResponse findReservationByPublicId(String publicId, Long currentUserId, boolean isAdmin);

    Page<SeatReservationResponse> findReservationsByUser(Long userId, SeatReservationStatus status, Long showtimeId, Pageable pageable);

    SeatAvailabilityResponse checkAvailability(Long showtimeId, List<Long> seatIds);

    com.lorafilm.booking.reservation.dto.PublicSeatAvailabilityResponse checkPublicAvailability(String showtimePublicId);

    com.lorafilm.booking.reservation.dto.OccupiedSeatsResponse getOccupiedSeatsByShowtime(String showtimeIdentifier);

    com.lorafilm.booking.reservation.dto.ExtendReservationResponse extendReservation(String publicId, Long userId);

    void handleBookingStatusChange(Long bookingId, com.lorafilm.booking.booking.enums.BookingStatus targetStatus, String reason);
}
