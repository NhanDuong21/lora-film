package com.lorafilm.movie.auditorium.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;

public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Auditorium> findByPublicIdAndStatusAndDeletedAtIsNull(String publicId, AuditoriumStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auditorium a WHERE a.publicId = :publicId AND a.deletedAt IS NULL")
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNullForUpdate(@Param("publicId") String publicId);

    boolean existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(Long cinemaId, String name);
    
    @Query("SELECT CASE WHEN count(a) > 0 THEN true ELSE false END FROM Auditorium a WHERE a.cinema.id = :cinemaId AND lower(a.name) = lower(:name) AND a.id != :excludeId AND a.deletedAt IS NULL")
    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(@Param("cinemaId") Long cinemaId, @Param("name") String name, @Param("excludeId") Long excludeId);
}
