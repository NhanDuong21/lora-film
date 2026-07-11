package com.lorafilm.movie.auditorium.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.common.enums.ActiveStatus;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    List<Auditorium> findByCinemaIdAndStatusAndDeletedAtIsNull(Long cinemaId, ActiveStatus status);
}
