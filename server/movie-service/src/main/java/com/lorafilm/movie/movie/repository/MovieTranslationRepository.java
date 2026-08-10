package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieTranslationRepository extends JpaRepository<MovieTranslation, Long> {
    List<MovieTranslation> findByMovieId(Long movieId);

    @Modifying
    @Query("DELETE FROM MovieTranslation t WHERE t.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
}
