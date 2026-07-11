package com.lorafilm.movie.auditorium.repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNull(String publicId);
    Optional<Auditorium> findByPublicIdAndStatusAndDeletedAtIsNull(String publicId, AuditoriumStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auditorium a WHERE a.publicId = :publicId AND a.deletedAt IS NULL")
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNullForUpdate(@Param("publicId") String publicId);

    boolean existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(Long cinemaId, String name);
    
    @Query("SELECT CASE WHEN count(a) > 0 THEN true ELSE false END FROM Auditorium a WHERE a.cinema.id = :cinemaId AND lower(a.name) = lower(:name) AND a.id != :excludeId AND a.deletedAt IS NULL")
    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(@Param("cinemaId") Long cinemaId, @Param("name") String name, @Param("excludeId") Long excludeId);

    List<Auditorium> findByCinemaIdAndStatusAndDeletedAtIsNull(Long cinemaId, ActiveStatus status);
}
