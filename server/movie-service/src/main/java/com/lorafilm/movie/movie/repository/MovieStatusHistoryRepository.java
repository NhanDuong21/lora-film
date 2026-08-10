package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieStatusHistoryRepository extends JpaRepository<MovieStatusHistory, Long> {
    List<MovieStatusHistory> findByMovieIdOrderByChangedAtDescIdDesc(Long movieId);
}
