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
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    Optional<Genre> findBySlugAndDeletedAtIsNull(String slug);
    
    org.springframework.data.domain.Page<Genre> findByDeletedAtIsNull(org.springframework.data.domain.Pageable pageable);
    List<Genre> findByStatusAndDeletedAtIsNull(com.lorafilm.movie.common.enums.ActiveStatus status);
    
    @Query("SELECT g.slug FROM Genre g WHERE g.slug LIKE :slugPrefix% AND g.deletedAt IS NULL")
    List<String> findSlugsByPrefix(@Param("slugPrefix") String slugPrefix);
    
    List<Genre> findByPublicIdInAndDeletedAtIsNull(List<String> publicIds);
}
