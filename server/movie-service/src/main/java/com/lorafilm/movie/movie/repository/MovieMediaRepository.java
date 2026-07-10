package com.lorafilm.movie.movie.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieMedia;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;

@Repository
public interface MovieMediaRepository extends JpaRepository<MovieMedia, Long> {

    List<MovieMedia> findByMovieIdAndStatusAndDeletedAtIsNull(Long movieId, ActiveStatus status);

    Optional<MovieMedia> findFirstByMovieIdAndMediaTypeAndIsPrimaryTrueAndStatusAndDeletedAtIsNull(Long movieId, MovieMediaType mediaType, ActiveStatus status);
}
