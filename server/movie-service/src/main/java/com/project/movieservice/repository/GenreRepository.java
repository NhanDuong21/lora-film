package com.project.movieservice.repository;

import com.project.movieservice.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    
    List<Genre> findAllByOrderByGenreNameAsc();

    List<Genre> findByStatusOrderByGenreNameAsc(com.project.movieservice.enumtype.GenreStatus status);

    boolean existsByGenreNameIgnoreCase(String genreName);

    boolean existsByGenreNameIgnoreCaseAndIdNot(String genreName, Integer id);
}
