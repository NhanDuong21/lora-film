package com.lorafilm.movie.cinema.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.cinema.domain.entity.CinemaMedia;
import com.lorafilm.movie.common.enums.ActiveStatus;

@Repository
public interface CinemaMediaRepository extends JpaRepository<CinemaMedia, Long> {
    List<CinemaMedia> findByCinemaIdAndStatusAndDeletedAtIsNullOrderByDisplayOrderAsc(Long cinemaId, ActiveStatus status);
}
