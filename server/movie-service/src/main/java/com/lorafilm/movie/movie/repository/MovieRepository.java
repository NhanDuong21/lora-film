package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Movie> findBySlugAndDeletedAtIsNull(String slug);
}
