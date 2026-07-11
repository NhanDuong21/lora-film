package com.lorafilm.movie.seat.repository;

import com.lorafilm.movie.seat.domain.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByAuditoriumIdAndDeletedAtIsNull(Long auditoriumId);
}
