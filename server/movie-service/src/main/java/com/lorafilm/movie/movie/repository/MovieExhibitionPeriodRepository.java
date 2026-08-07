package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.MovieExhibitionPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MovieExhibitionPeriodRepository extends JpaRepository<MovieExhibitionPeriod, Long> {
    List<MovieExhibitionPeriod> findByMovieIdAndDeletedAtIsNullOrderByStartDateDescIdDesc(Long movieId);

    boolean existsByMovieIdAndStartDateAndEndDateAndDeletedAtIsNull(
            Long movieId, LocalDate startDate, LocalDate endDate);
}
