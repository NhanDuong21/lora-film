package com.lorafilm.movie.movie.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.movie.domain.entity.MovieProductionCompany;
import com.lorafilm.movie.movie.domain.entity.MovieProductionCompanyId;

@Repository
public interface MovieProductionCompanyRepository extends JpaRepository<MovieProductionCompany, MovieProductionCompanyId> {
    List<MovieProductionCompany> findByMovieId(Long movieId);
}
