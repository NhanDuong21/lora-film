package com.lorafilm.movie.auditorium.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.auditorium.domain.entity.Auditorium;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface AuditoriumRepository extends JpaRepository<Auditorium, Long> {
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNull(String publicId);
    
    List<Auditorium> findByPublicIdInAndDeletedAtIsNull(List<String> publicIds);

    Optional<Auditorium> findByPublicIdAndStatusAndDeletedAtIsNull(String publicId, AuditoriumStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auditorium a WHERE a.publicId = :publicId AND a.deletedAt IS NULL")
    Optional<Auditorium> findByPublicIdAndDeletedAtIsNullForUpdate(@Param("publicId") String publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select a from Auditorium a where a.id = :auditoriumId and a.deletedAt is null")
    Optional<Auditorium> findByIdForScheduling(@Param("auditoriumId") Long auditoriumId);

    boolean existsByCinemaIdAndNameIgnoreCaseAndDeletedAtIsNull(Long cinemaId, String name);

    @Query("SELECT CASE WHEN count(a) > 0 THEN true ELSE false END FROM Auditorium a WHERE a.cinema.id = :cinemaId AND lower(a.name) = lower(:name) AND a.id != :excludeId AND a.deletedAt IS NULL")
    boolean existsByCinemaIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(@Param("cinemaId") Long cinemaId,
            @Param("name") String name, @Param("excludeId") Long excludeId);

    List<Auditorium> findByCinemaIdAndStatusAndDeletedAtIsNull(Long cinemaId, AuditoriumStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(@jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select a from Auditorium a where a.id in :ids and a.deletedAt is null order by a.id asc")
    List<Auditorium> findAllByIdForScheduling(@Param("ids") List<Long> ids);
}
