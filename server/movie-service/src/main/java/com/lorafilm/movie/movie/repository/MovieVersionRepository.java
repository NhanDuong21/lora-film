package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieVersionRepository extends JpaRepository<MovieVersion, Long> {
    Optional<MovieVersion> findByPublicIdAndDeletedAtIsNull(String publicId);
    
    List<MovieVersion> findByMovieIdAndDeletedAtIsNull(Long movieId);
    
    List<MovieVersion> findByMovieIdAndStatusAndDeletedAtIsNull(Long movieId, ActiveStatus status);

    boolean existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguage(
        Long movieId, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage
    );

    boolean existsByMovieIdAndFormatAndAudioLanguageAndSubtitleLanguageAndDubLanguageAndIdNot(
        Long movieId, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage, Long id
    );

    @Query("SELECT COUNT(v) > 0 FROM MovieVersion v WHERE v.movie.id = :movieId AND v.status = com.lorafilm.movie.common.enums.ActiveStatus.ACTIVE AND v.deletedAt IS NULL")
    boolean existsActiveVersion(@Param("movieId") Long movieId);
}
