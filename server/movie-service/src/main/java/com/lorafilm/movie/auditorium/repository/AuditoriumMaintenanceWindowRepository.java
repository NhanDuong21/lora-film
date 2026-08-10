package com.lorafilm.movie.auditorium.repository;

import com.lorafilm.movie.auditorium.domain.entity.AuditoriumMaintenanceWindow;
import com.lorafilm.movie.common.enums.ActionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AuditoriumMaintenanceWindowRepository extends JpaRepository<AuditoriumMaintenanceWindow, Long> {
    @Query("SELECT mw FROM AuditoriumMaintenanceWindow mw WHERE mw.auditorium.id = :auditoriumId ORDER BY mw.startTime DESC")
    List<AuditoriumMaintenanceWindow> findByAuditoriumIdOrderByStartTimeDesc(@Param("auditoriumId") Long auditoriumId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mw FROM AuditoriumMaintenanceWindow mw WHERE mw.id = :id")
    Optional<AuditoriumMaintenanceWindow> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT CASE WHEN count(mw) > 0 THEN true ELSE false END " +
           "FROM AuditoriumMaintenanceWindow mw " +
           "WHERE mw.auditorium.id = :auditoriumId " +
           "  AND mw.status = :activeStatus " +
           "  AND mw.startTime < :endTime " +
           "  AND mw.endTime > :startTime")
    boolean existsOverlap(@Param("auditoriumId") Long auditoriumId,
                          @Param("activeStatus") ActionStatus activeStatus,
                          @Param("startTime") Instant startTime,
                          @Param("endTime") Instant endTime);

    @Query("SELECT mw " +
           "FROM AuditoriumMaintenanceWindow mw " +
           "WHERE mw.auditorium.id = :auditoriumId " +
           "  AND mw.status = :activeStatus " +
           "  AND mw.startTime < :endTime " +
           "  AND mw.endTime > :startTime " +
           "ORDER BY mw.startTime ASC LIMIT 1")
    Optional<AuditoriumMaintenanceWindow> findFirstOverlap(@Param("auditoriumId") Long auditoriumId,
                                                           @Param("activeStatus") ActionStatus activeStatus,
                                                           @Param("startTime") Instant startTime,
                                                           @Param("endTime") Instant endTime);

    @Query("SELECT mw " +
           "FROM AuditoriumMaintenanceWindow mw " +
           "WHERE mw.auditorium.id = :auditoriumId " +
           "  AND mw.id <> :excludedId " +
           "  AND mw.status = :activeStatus " +
           "  AND mw.startTime < :endTime " +
           "  AND mw.endTime > :startTime " +
           "ORDER BY mw.startTime ASC LIMIT 1")
    Optional<AuditoriumMaintenanceWindow> findFirstOverlapExcluding(
            @Param("auditoriumId") Long auditoriumId,
            @Param("excludedId") Long excludedId,
            @Param("activeStatus") ActionStatus activeStatus,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    @Query("SELECT mw FROM AuditoriumMaintenanceWindow mw JOIN FETCH mw.auditorium " +
           "WHERE mw.auditorium.id IN :auditoriumIds " +
           "AND mw.status = :activeStatus " +
           "AND mw.startTime < :endTime " +
           "AND mw.endTime > :startTime " +
           "ORDER BY mw.auditorium.id ASC, mw.startTime ASC")
    List<AuditoriumMaintenanceWindow> findActiveOverlapsForAutoSchedule(
            @Param("auditoriumIds") List<Long> auditoriumIds,
            @Param("activeStatus") ActionStatus activeStatus,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
