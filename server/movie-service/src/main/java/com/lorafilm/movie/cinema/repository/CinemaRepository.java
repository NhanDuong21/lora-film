package com.lorafilm.movie.cinema.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long>, JpaSpecificationExecutor<Cinema> {
    Optional<Cinema> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Cinema> findByActiveSlugAndDeletedAtIsNull(String slug);
}
