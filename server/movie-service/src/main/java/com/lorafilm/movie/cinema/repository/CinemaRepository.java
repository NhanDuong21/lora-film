package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    Optional<Cinema> findByPublicIdAndDeletedAtIsNull(String publicId);
}
