package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.CinemaClosurePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CinemaClosurePeriodRepository extends JpaRepository<CinemaClosurePeriod, Long> {

    List<CinemaClosurePeriod> findByCinemaId(Long cinemaId);

    @Query("SELECT c FROM CinemaClosurePeriod c WHERE c.cinema.id = :cinemaId " +
            "AND c.status = 'ACTIVE' " +
            "AND (c.startTime < :endTime AND c.endTime > :startTime)")
    List<CinemaClosurePeriod> findOverlappingClosures(
            @Param("cinemaId") Long cinemaId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
