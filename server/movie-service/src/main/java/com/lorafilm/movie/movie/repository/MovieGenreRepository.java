package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieGenre;
import com.lorafilm.movie.movie.domain.entity.MovieGenreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieGenreRepository extends JpaRepository<MovieGenre, MovieGenreId> {
    List<MovieGenre> findByMovieId(Long movieId);
    List<MovieGenre> findByMovieIdIn(List<Long> movieIds);
    void deleteByMovieId(Long movieId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(mg) > 0 FROM MovieGenre mg JOIN mg.movie m WHERE mg.genre.id = :genreId AND m.deletedAt IS NULL")
    boolean existsByGenreIdAndMovieDeletedAtIsNull(@org.springframework.data.repository.query.Param("genreId") Long genreId);
}
