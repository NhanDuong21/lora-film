package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    Optional<Showtime> findByPublicIdAndDeletedAtIsNull(String publicId);
}
