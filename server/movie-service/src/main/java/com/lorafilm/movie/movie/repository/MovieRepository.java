package com.lorafilm.movie.movie.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lorafilm.movie.movie.domain.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Movie> findBySlugAndDeletedAtIsNull(String slug);
}

