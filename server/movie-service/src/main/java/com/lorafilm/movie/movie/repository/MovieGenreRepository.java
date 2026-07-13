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
}
