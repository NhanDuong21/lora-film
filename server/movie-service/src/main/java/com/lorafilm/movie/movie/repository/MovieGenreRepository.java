package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenreId> {
    List<MovieGenre> findByMovieId(Long movieId);
    List<MovieGenre> findByMovieIdIn(List<Long> movieIds);
    
    @Modifying
    @Query("DELETE FROM MovieGenre mg WHERE mg.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
    
    boolean existsByMovieIdAndGenreId(Long movieId, Long genreId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(mg) > 0 FROM MovieGenre mg JOIN mg.movie m WHERE mg.genre.id = :genreId AND m.deletedAt IS NULL")
    boolean existsByGenreIdAndMovieDeletedAtIsNull(@org.springframework.data.repository.query.Param("genreId") Long genreId);
}
