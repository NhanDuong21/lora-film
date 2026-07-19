package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieTranslationRepository extends JpaRepository<MovieTranslation, Long> {
    void deleteByMovieId(Long movieId);
}
