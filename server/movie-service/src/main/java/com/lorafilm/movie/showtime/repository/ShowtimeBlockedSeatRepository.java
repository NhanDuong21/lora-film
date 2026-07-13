package com.lorafilm.movie.showtime.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;

@Repository
public interface ShowtimeBlockedSeatRepository extends JpaRepository<ShowtimeBlockedSeat, Long> {
    List<ShowtimeBlockedSeat> findByShowtimeIdAndStatus(Long showtimeId, ActionStatus status);
}
