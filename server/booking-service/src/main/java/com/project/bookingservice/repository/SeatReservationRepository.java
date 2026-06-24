package com.project.bookingservice.repository;

import com.project.bookingservice.entity.SeatReservation;
import com.project.bookingservice.enumtype.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long> {
    
    List<SeatReservation> findByShowtimeIdAndSeatIdInAndStatus(Long showtimeId, List<Long> seatIds, ReservationStatus status);

    boolean existsByShowtimeIdAndSeatIdAndStatus(Long showtimeId, Long seatId, ReservationStatus status);
}
