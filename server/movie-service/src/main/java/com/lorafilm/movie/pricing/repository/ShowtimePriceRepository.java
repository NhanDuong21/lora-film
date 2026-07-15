package com.lorafilm.movie.pricing.repository;

import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("pricingShowtimePriceRepository")
public interface ShowtimePriceRepository extends JpaRepository<ShowtimePrice, Long> {

    List<ShowtimePrice> findByShowtimeId(Long showtimeId);

    @Query("SELECT sp FROM ShowtimePrice sp JOIN FETCH sp.seatType WHERE sp.showtime.id = :showtimeId")
    List<ShowtimePrice> findByShowtimeIdWithSeatType(@Param("showtimeId") Long showtimeId);

    Optional<ShowtimePrice> findByShowtimeIdAndSeatTypeId(Long showtimeId, Long seatTypeId);

}
