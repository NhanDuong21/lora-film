package com.lorafilm.movie.movie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.MovieCredit;

@Repository
public interface MovieCreditRepository extends JpaRepository<MovieCredit, Long> {
    List<MovieCredit> findByMovieIdAndDeletedAtIsNullOrderByDisplayOrderAsc(Long movieId);
    
    @Modifying
    @Query("DELETE FROM MovieCredit c WHERE c.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
    
    boolean existsByMovieIdAndPersonIdAndRoleTypeAndDeletedAtIsNull(Long movieId, Long personId, com.lorafilm.movie.movie.domain.enums.CreditRoleType role);
}
