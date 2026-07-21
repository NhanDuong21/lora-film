package com.lorafilm.booking.domain.repository;

import com.lorafilm.booking.domain.entity.SeatReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatReservationRepository extends JpaRepository<SeatReservation, Long>, JpaSpecificationExecutor<SeatReservation> {
    Optional<SeatReservation> findByPublicId(UUID publicId);
    Optional<SeatReservation> findByReservationToken(UUID reservationToken);
}
