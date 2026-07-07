package com.project.analyticsservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.analyticsservice.entity.MovieRevenueStat;

@Repository
public interface MovieRevenueStatRepository extends JpaRepository<MovieRevenueStat, Long> {

    Optional<MovieRevenueStat> findByMovieId(Long movieId);

    boolean existsByMovieId(Long movieId);

    Page<MovieRevenueStat> findByMovieTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("SELECT m FROM MovieRevenueStat m WHERE " +
           "(:movieId IS NULL OR m.movieId = :movieId) AND " +
           "(:movieTitle IS NULL OR LOWER(m.movieTitle) LIKE LOWER(CONCAT('%', :movieTitle, '%')))")
    Page<MovieRevenueStat> searchLifetime(
            @Param("movieId") Long movieId,
            @Param("movieTitle") String movieTitle,
            Pageable pageable);

    List<MovieRevenueStat> findTop10ByOrderByTotalRevenueDesc();

    List<MovieRevenueStat> findTop10ByOrderByTotalTicketsSoldDesc();
}
