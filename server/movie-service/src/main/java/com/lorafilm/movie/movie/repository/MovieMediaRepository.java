package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MovieMediaRepository extends JpaRepository<MovieMedia, Long> {

    Optional<MovieMedia> findByPublicIdAndDeletedAtIsNull(String publicId);

    @Query("SELECT m FROM MovieMedia m WHERE m.movie.id = :movieId AND m.deletedAt IS NULL ORDER BY m.displayOrder ASC, m.createdAt DESC, m.id ASC")
    List<MovieMedia> findByMovieIdAndDeletedAtIsNull(@Param("movieId") Long movieId);

    @Query("SELECT m FROM MovieMedia m WHERE m.movie.id = :movieId AND m.status = :status AND m.deletedAt IS NULL ORDER BY m.displayOrder ASC, m.createdAt DESC, m.id ASC")
    List<MovieMedia> findByMovieIdAndStatusAndDeletedAtIsNull(@Param("movieId") Long movieId, @Param("status") ActiveStatus status);

    Optional<MovieMedia> findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(Long movieId, MovieMediaType mediaType, ActiveStatus status);

    List<MovieMedia> findByMovieIdInAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(List<Long> movieIds, MovieMediaType mediaType, ActiveStatus status);

    @Query("SELECT m FROM MovieMedia m WHERE m.movie.id = :movieId AND m.mediaType = :mediaType AND m.isPrimary = true AND m.deletedAt IS NULL")
    Optional<MovieMedia> findPrimaryMedia(@Param("movieId") Long movieId, @Param("mediaType") MovieMediaType mediaType);

    @Query("SELECT COUNT(m) > 0 FROM MovieMedia m WHERE m.movie.id = :movieId AND m.mediaType = :mediaType AND m.isPrimary = true AND m.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND m.deletedAt IS NULL")
    boolean existsPrimaryMedia(@Param("movieId") Long movieId, @Param("mediaType") MovieMediaType mediaType);

    @Query("SELECT COUNT(m) > 0 FROM MovieMedia m WHERE m.movie.id = :movieId AND m.mediaType = com.lorafilm.movie.movie.domain.enums.MovieMediaType.POSTER AND m.isPrimary = true AND m.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND m.deletedAt IS NULL")
    boolean existsPrimaryPoster(@Param("movieId") Long movieId);

    @Modifying
    @Query("UPDATE MovieMedia m SET m.isPrimary = false WHERE m.movie.id = :movieId AND m.mediaType = :mediaType AND m.isPrimary = true AND m.deletedAt IS NULL")
    void resetPrimaryMedia(@Param("movieId") Long movieId, @Param("mediaType") MovieMediaType mediaType);

    @Modifying
    @Query("DELETE FROM MovieMedia m WHERE m.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);

    @Query("SELECT COUNT(m) FROM MovieMedia m WHERE m.movie.id = :movieId AND m.deletedAt IS NULL")
    long countMedia(@Param("movieId") Long movieId);

    @Query("SELECT m.movie.id, COUNT(m) FROM MovieMedia m WHERE m.movie.id IN :movieIds AND m.deletedAt IS NULL GROUP BY m.movie.id")
    List<Object[]> countMediaByMovieIds(@Param("movieIds") List<Long> movieIds);

    @Query("SELECT m.movie.id, COUNT(m) FROM MovieMedia m WHERE m.movie.id IN :movieIds AND m.mediaType = com.lorafilm.movie.movie.domain.enums.MovieMediaType.POSTER AND m.isPrimary = true AND m.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND m.deletedAt IS NULL GROUP BY m.movie.id")
    List<Object[]> countPrimaryPostersByMovieIds(@Param("movieIds") List<Long> movieIds);
}
