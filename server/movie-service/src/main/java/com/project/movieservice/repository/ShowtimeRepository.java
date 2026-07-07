package com.project.movieservice.repository;

import com.project.movieservice.entity.Showtime;
import com.project.movieservice.enumtype.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    
    boolean existsByRoomIdAndStartTimeAfterAndStatusIn(Integer roomId, LocalDateTime time, List<ShowtimeStatus> statuses);

    boolean existsByRoomIdAndEndTimeAfter(Integer roomId, LocalDateTime time);

    boolean existsByMovieIdAndEndTimeAfter(Long movieId, LocalDateTime time);
}
