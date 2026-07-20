package com.lorafilm.movie.movie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompanyId;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface MovieProductionCompanyRepository extends JpaRepository<MovieProductionCompany, MovieProductionCompanyId> {
    List<MovieProductionCompany> findByMovieId(Long movieId);
    
    @Modifying
    @Query("DELETE FROM MovieProductionCompany mpc WHERE mpc.movie.id = :movieId")
    void deleteByMovieId(@Param("movieId") Long movieId);
    
    boolean existsByMovieIdAndProductionCompanyIdAndRole(Long movieId, Long productionCompanyId, com.lorafilm.movie.movie.domain.enums.CompanyRoleType role);
}
