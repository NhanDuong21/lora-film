package com.lorafilm.movie.movie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.entity.MovieVersion;

@Repository
public interface MovieVersionRepository extends JpaRepository<MovieVersion, Long> {
    List<MovieVersion> findByMovieIdAndStatusAndDeletedAtIsNull(Long movieId, ActiveStatus status);
}
