package com.lorafilm.movie.cinema.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.cinema.domain.entity.CinemaOperatingHour;

@Repository
public interface CinemaOperatingHourRepository extends JpaRepository<CinemaOperatingHour, Long> {
    List<CinemaOperatingHour> findByCinemaId(Long cinemaId);
}
