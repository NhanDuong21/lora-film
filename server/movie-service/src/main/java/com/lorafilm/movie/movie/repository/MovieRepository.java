package com.lorafilm.movie.movie.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import com.lorafilm.movie.movie.domain.entity.Movie;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    Optional<Movie> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movie m WHERE m.publicId = :publicId AND m.deletedAt IS NULL")
    Optional<Movie> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movie m WHERE m.status = :status AND m.releaseDate <= :today AND m.deletedAt IS NULL ORDER BY m.id ASC")
    List<Movie> findReleasedByStatusForUpdate(
            @Param("status") MovieStatus status,
            @Param("today") java.time.LocalDate today);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Movie m WHERE m.status = :status AND m.endDate IS NOT NULL AND m.endDate < :today AND m.deletedAt IS NULL ORDER BY m.id ASC")
    List<Movie> findEndedByStatusForUpdate(
            @Param("status") MovieStatus status,
            @Param("today") java.time.LocalDate today);
    Optional<Movie> findBySlugAndDeletedAtIsNull(String slug);
    Optional<Movie> findByTmdbId(Long tmdbId);
    List<Movie> findByTmdbIdIn(List<Long> tmdbIds);
    
    @Query("SELECT m FROM Movie m WHERE (m.publicId = :identifier OR m.slug = :identifier) AND m.deletedAt IS NULL")
    Optional<Movie> findByIdentifierAndDeletedAtIsNull(@Param("identifier") String identifier);
    
    Page<Movie> findByStatusAndDeletedAtIsNull(MovieStatus status, Pageable pageable);

    List<Movie> findByStatusInAndDeletedAtIsNull(List<MovieStatus> statuses);

    @Query("SELECT m.status AS status, COUNT(m) AS total FROM Movie m WHERE m.deletedAt IS NULL GROUP BY m.status")
    List<MovieStatusCountProjection> countNonDeletedMoviesByStatus();
    
    @Query("SELECT m.slug FROM Movie m WHERE m.slug LIKE :slugPrefix% AND m.deletedAt IS NULL")
    List<String> findSlugsByPrefix(@Param("slugPrefix") String slugPrefix);
}

