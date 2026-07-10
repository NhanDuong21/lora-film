package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieMediaRepository extends JpaRepository<MovieMedia, Long> {
    List<MovieMedia> findByMovieIdAndStatusAndDeletedAtIsNull(Long movieId, ActiveStatus status);
    Optional<MovieMedia> findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(Long movieId, MovieMediaType mediaType, ActiveStatus status);
}
