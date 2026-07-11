package com.lorafilm.movie.showtime.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;

@Repository
public interface ShowtimePriceRepository extends JpaRepository<ShowtimePrice, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seatType"})
    List<ShowtimePrice> findByShowtimeId(Long showtimeId);
}
