package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;

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
}
