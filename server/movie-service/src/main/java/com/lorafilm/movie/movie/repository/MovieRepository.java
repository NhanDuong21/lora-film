package com.lorafilm.movie.movie.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Movie> findBySlugAndDeletedAtIsNull(String slug);
    
    @Query("SELECT m FROM Movie m WHERE (m.publicId = :identifier OR m.activeSlug = :identifier) AND m.deletedAt IS NULL")
    Optional<Movie> findByIdentifierAndDeletedAtIsNull(@Param("identifier") String identifier);
    
    Page<Movie> findByStatusAndDeletedAtIsNull(MovieStatus status, Pageable pageable);
    
    @Query("SELECT m.activeSlug FROM Movie m WHERE m.activeSlug LIKE :slugPrefix% AND m.deletedAt IS NULL")
    List<String> findActiveSlugsByPrefix(@Param("slugPrefix") String slugPrefix);
}

