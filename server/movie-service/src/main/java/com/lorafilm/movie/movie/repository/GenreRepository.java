package com.lorafilm.movie.movie.repository;

import com.lorafilm.movie.movie.domain.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByActiveSlugAndDeletedAtIsNull(String activeSlug);
    Optional<Genre> findByActiveSlugAndDeletedAtIsNull(String activeSlug);
    
    @Query("SELECT g.activeSlug FROM Genre g WHERE g.activeSlug LIKE :slugPrefix% AND g.deletedAt IS NULL")
    List<String> findActiveSlugsByPrefix(@Param("slugPrefix") String slugPrefix);
    
    List<Genre> findByPublicIdInAndDeletedAtIsNull(List<String> publicIds);
}
