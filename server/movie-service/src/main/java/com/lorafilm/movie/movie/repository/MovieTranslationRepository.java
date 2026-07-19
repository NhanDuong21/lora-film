package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieTranslationRepository extends JpaRepository<MovieTranslation, Long> {
    @Modifying
    @Query("DELETE FROM MovieTranslation t WHERE t.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}
