package com.lorafilm.movie.pricing.repository;

import com.lorafilm.movie.pricing.domain.entity.ShowtimePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Repository("pricingShowtimePriceRepository")
public interface ShowtimePriceRepository extends JpaRepository<ShowtimePrice, Long> {

    List<ShowtimePrice> findByShowtimeId(Long showtimeId);

    @Query("SELECT sp FROM ShowtimePrice sp JOIN FETCH sp.seatType " +
           "LEFT JOIN FETCH sp.sourcePolicy LEFT JOIN FETCH sp.sourceRule " +
           "WHERE sp.showtime.id = :showtimeId ORDER BY sp.seatType.publicId")
    List<ShowtimePrice> findByShowtimeIdWithSeatType(@Param("showtimeId") Long showtimeId);

    @Query("SELECT sp FROM ShowtimePrice sp JOIN FETCH sp.showtime JOIN FETCH sp.seatType " +
           "WHERE sp.showtime.id IN :showtimeIds")
    List<ShowtimePrice> findByShowtimeIdInWithSeatType(
            @Param("showtimeIds") Collection<Long> showtimeIds);

    Optional<ShowtimePrice> findByShowtimeIdAndSeatTypeId(Long showtimeId, Long seatTypeId);

    void deleteByShowtimeId(Long showtimeId);

    @Query("select count(distinct sp.showtime.id) from ShowtimePrice sp where sp.sourcePolicy.publicId = :policyPublicId")
    long countDistinctShowtimesBySourcePolicyPublicId(@Param("policyPublicId") String policyPublicId);

}
