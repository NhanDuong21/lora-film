package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    boolean existsByAuditoriumId(Long auditoriumId);
    boolean existsByMovieIdAndDeletedAtIsNull(Long movieId);

    Optional<Showtime> findByPublicIdAndDeletedAtIsNull(String publicId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s " +
            "JOIN FETCH s.movie " +
            "JOIN FETCH s.movieVersion " +
            "JOIN FETCH s.cinema " +
            "JOIN FETCH s.auditorium " +
            "WHERE s.publicId = :publicId " +
            "AND s.deletedAt IS NULL")
    Optional<Showtime> findByPublicIdForUpdate(
            @org.springframework.data.repository.query.Param("publicId") String publicId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"movie", "movieVersion", "cinema", "auditorium"})
    Optional<Showtime> findByIdAndDeletedAtIsNull(Long id);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id = :auditoriumId " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " + // Exclude cancelled showtimes from overlap check
            "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    java.util.List<Showtime> findPotentialOverlaps(
            @org.springframework.data.repository.query.Param("auditoriumId") Long auditoriumId,
            @org.springframework.data.repository.query.Param("startTime") java.time.Instant startTime,
            @org.springframework.data.repository.query.Param("endTime") java.time.Instant endTime);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id = :auditoriumId " +
            "AND s.id != :excludeShowtimeId " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    java.util.List<Showtime> findPotentialOverlaps(
            @org.springframework.data.repository.query.Param("auditoriumId") Long auditoriumId,
            @org.springframework.data.repository.query.Param("startTime") java.time.Instant startTime,
            @org.springframework.data.repository.query.Param("endTime") java.time.Instant endTime,
            @org.springframework.data.repository.query.Param("excludeShowtimeId") Long excludeShowtimeId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id IN :auditoriumIds " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND s.startTime >= :fromTime AND s.startTime <= :toTime " +
            "ORDER BY s.startTime ASC")
    java.util.List<Showtime> findByAuditoriumIdInAndStartTimeBetween(
            @org.springframework.data.repository.query.Param("auditoriumIds") java.util.List<Long> auditoriumIds,
            @org.springframework.data.repository.query.Param("fromTime") java.time.Instant fromTime,
            @org.springframework.data.repository.query.Param("toTime") java.time.Instant toTime);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id = :auditoriumId " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND s.startTime < :occupancyEndTime AND s.endTime > :candidateStartMinusBuffer")
    java.util.List<Showtime> findBlockingOverlapsForScheduling(
            @org.springframework.data.repository.query.Param("auditoriumId") Long auditoriumId,
            @org.springframework.data.repository.query.Param("candidateStartMinusBuffer") java.time.Instant candidateStartMinusBuffer,
            @org.springframework.data.repository.query.Param("occupancyEndTime") java.time.Instant occupancyEndTime);
}
