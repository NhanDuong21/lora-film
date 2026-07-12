package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.ShowtimeStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowtimeStatusHistoryRepository extends JpaRepository<ShowtimeStatusHistory, Long> {
    List<ShowtimeStatusHistory> findByShowtimeId(Long showtimeId);
}
