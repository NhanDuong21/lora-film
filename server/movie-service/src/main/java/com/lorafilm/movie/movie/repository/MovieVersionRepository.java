package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public interface MovieVersionRepository extends JpaRepository<MovieVersion, Long> {
    Optional<MovieVersion> findByPublicIdAndDeletedAtIsNull(String publicId);
    
    List<MovieVersion> findByPublicIdInAndDeletedAtIsNull(List<String> publicIds);
    
    @Query("SELECT mv FROM MovieVersion mv JOIN FETCH mv.movie WHERE mv.publicId IN :publicIds AND mv.deletedAt IS NULL")
    List<MovieVersion> findByPublicIdInWithMovieAndDeletedAtIsNull(@Param("publicIds") List<String> publicIds);
    
    List<MovieVersion> findByMovieIdAndDeletedAtIsNull(Long movieId);
    
    List<MovieVersion> findByMovieIdAndStatusAndDeletedAtIsNull(Long movieId, ActiveStatus status);

    @Query("""
            select mv from MovieVersion mv
            join fetch mv.movie m
            where mv.status = :versionStatus
              and mv.deletedAt is null
              and m.status in :movieStatuses
              and m.deletedAt is null
              and m.durationMinutes > 0
              and (m.releaseDate is null or m.releaseDate <= :toDate)
              and (m.endDate is null or m.endDate >= :fromDate)
            order by m.id asc, mv.id asc
            """)
    List<MovieVersion> findEligibleForAutoSchedule(
            @Param("versionStatus") ActiveStatus versionStatus,
            @Param("movieStatuses") List<MovieStatus> movieStatuses,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate);

    boolean existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguage(
        Long movieId, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage
    );

    boolean existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguageAndIdNot(
        Long movieId, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage, Long id
    );

    @Query("SELECT COUNT(v) > 0 FROM MovieVersion v WHERE v.movie.id = :movieId AND v.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND v.deletedAt IS NULL")
    boolean existsActiveVersion(@Param("movieId") Long movieId);

    @Query("SELECT COUNT(v) FROM MovieVersion v WHERE v.movie.id = :movieId AND v.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND v.deletedAt IS NULL")
    long countActiveVersions(@Param("movieId") Long movieId);

    @Query("SELECT v.movie.id, COUNT(v) FROM MovieVersion v WHERE v.movie.id IN :movieIds AND v.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND v.deletedAt IS NULL GROUP BY v.movie.id")
    List<Object[]> countActiveVersionsByMovieIds(@Param("movieIds") List<Long> movieIds);
}
