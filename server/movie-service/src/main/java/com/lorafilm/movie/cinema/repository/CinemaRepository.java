package com.lorafilm.movie.cinema.repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long>, JpaSpecificationExecutor<Cinema> {
    Optional<Cinema> findBySlugAndDeletedAtIsNull(String slug);
}
