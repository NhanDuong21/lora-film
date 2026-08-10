package com.lorafilm.movie.showtime.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActionStatus;
import com.lorafilm.movie.showtime.domain.entity.ShowtimeBlockedSeat;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

@Repository
public interface ShowtimeBlockedSeatRepository extends JpaRepository<ShowtimeBlockedSeat, Long> {
    List<ShowtimeBlockedSeat> findByShowtimeIdAndStatus(Long showtimeId, ActionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select blocked from ShowtimeBlockedSeat blocked
            join fetch blocked.seat seat
            where blocked.showtime.id = :showtimeId
              and seat.id in :seatIds
            order by blocked.id asc
            """)
    List<ShowtimeBlockedSeat> findForUpdate(
            @Param("showtimeId") Long showtimeId,
            @Param("seatIds") List<Long> seatIds);
}
