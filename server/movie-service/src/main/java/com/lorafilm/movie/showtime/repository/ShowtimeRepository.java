package com.lorafilm.movie.showtime.repository;

import com.lorafilm.movie.showtime.domain.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Collection;
import java.time.Instant;
import java.time.LocalDate;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long>, JpaSpecificationExecutor<Showtime> {
    boolean existsByAuditoriumId(Long auditoriumId);
    boolean existsByMovieIdAndDeletedAtIsNull(Long movieId);
    boolean existsByMovieIdAndStatusAndStartTimeAfterAndDeletedAtIsNull(
            Long movieId,
            com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus status,
            Instant startTime);
    boolean existsByMovieIdAndStatusInAndEndTimeAfterAndDeletedAtIsNull(
            Long movieId,
            Collection<com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus> statuses,
            Instant endTime);

    Optional<Showtime> findByPublicIdAndDeletedAtIsNull(String publicId);

    @org.springframework.data.jpa.repository.Query("""
            select s from Showtime s
            join fetch s.movie m
            join fetch s.movieVersion
            join fetch s.cinema
            join fetch s.auditorium
            where (m.slug = :identifier or m.publicId = :identifier)
              and m.deletedAt is null
              and s.deletedAt is null
              and s.status = com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.OPEN_FOR_BOOKING
              and s.serviceDate between :fromDate and :toDate
              and s.startTime > :now
            order by s.serviceDate asc, s.startTime asc, s.publicId asc
            """)
    List<Showtime> findCustomerBookingOptions(
            @org.springframework.data.repository.query.Param("identifier") String identifier,
            @org.springframework.data.repository.query.Param("fromDate") LocalDate fromDate,
            @org.springframework.data.repository.query.Param("toDate") LocalDate toDate,
            @org.springframework.data.repository.query.Param("now") Instant now);

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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s " +
            "JOIN FETCH s.movie " +
            "JOIN FETCH s.movieVersion " +
            "JOIN FETCH s.cinema " +
            "JOIN FETCH s.auditorium " +
            "WHERE s.batchId = :batchId " +
            "AND s.deletedAt IS NULL " +
            "ORDER BY s.id ASC")
    List<Showtime> findAllByBatchIdForUpdate(
            @org.springframework.data.repository.query.Param("batchId") String batchId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"movie", "movieVersion", "cinema", "auditorium"})
    List<Showtime> findAllByBatchIdAndDeletedAtIsNullOrderByIdAsc(String batchId);

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

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s JOIN FETCH s.auditorium " +
            "WHERE s.auditorium.id IN :auditoriumIds " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND s.startTime < :upperStartExclusive " +
            "AND s.endTime > :lowerEndExclusive " +
            "ORDER BY s.auditorium.id ASC, s.startTime ASC")
    java.util.List<Showtime> findBlockingFactsForAutoSchedule(
            @org.springframework.data.repository.query.Param("auditoriumIds") java.util.List<Long> auditoriumIds,
            @org.springframework.data.repository.query.Param("lowerEndExclusive") java.time.Instant lowerEndExclusive,
            @org.springframework.data.repository.query.Param("upperStartExclusive") java.time.Instant upperStartExclusive);

    @org.springframework.data.jpa.repository.Query("SELECT s.movie.id AS movieId, " +
            "s.movie.publicId AS moviePublicId, s.startTime AS startTime " +
            "FROM Showtime s " +
            "WHERE s.cinema.id = :cinemaId " +
            "AND s.movie.id IN :movieIds " +
            "AND s.deletedAt IS NULL " +
            "AND s.status IN :statuses " +
            "AND s.startTime >= :planningStart " +
            "AND s.startTime < :planningEndExclusive " +
            "ORDER BY s.startTime ASC, s.movie.publicId ASC")
    java.util.List<AutoScheduleExistingShowtimeFact> findCoverageFactsForAutoSchedule(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("movieIds") java.util.List<Long> movieIds,
            @org.springframework.data.repository.query.Param("statuses")
            java.util.List<com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus> statuses,
            @org.springframework.data.repository.query.Param("planningStart") java.time.Instant planningStart,
            @org.springframework.data.repository.query.Param("planningEndExclusive")
            java.time.Instant planningEndExclusive);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id = :auditoriumId " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND s.startTime < :occupancyEndTime AND s.endTime > :candidateStartMinusBuffer")
    java.util.List<Showtime> findBlockingOverlapsForScheduling(
            @org.springframework.data.repository.query.Param("auditoriumId") Long auditoriumId,
            @org.springframework.data.repository.query.Param("candidateStartMinusBuffer") java.time.Instant candidateStartMinusBuffer,
            @org.springframework.data.repository.query.Param("occupancyEndTime") java.time.Instant occupancyEndTime);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Showtime s WHERE s.auditorium.id = :auditoriumId " +
            "AND s.id != :excludeShowtimeId " +
            "AND s.deletedAt IS NULL " +
            "AND s.status != 'CANCELLED' " +
            "AND s.startTime < :occupancyEndTime AND s.endTime > :candidateStartMinusBuffer")
    java.util.List<Showtime> findBlockingOverlapsForScheduling(
            @org.springframework.data.repository.query.Param("auditoriumId") Long auditoriumId,
            @org.springframework.data.repository.query.Param("candidateStartMinusBuffer") java.time.Instant candidateStartMinusBuffer,
            @org.springframework.data.repository.query.Param("occupancyEndTime") java.time.Instant occupancyEndTime,
            @org.springframework.data.repository.query.Param("excludeShowtimeId") Long excludeShowtimeId);

    boolean existsByCinemaIdAndDeletedAtIsNull(Long cinemaId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Showtime s WHERE s.movie.id = :movieId AND s.deletedAt IS NULL")
    long countShowtimes(@org.springframework.data.repository.query.Param("movieId") Long movieId);

    @org.springframework.data.jpa.repository.Query("SELECT s.movie.id, COUNT(s) FROM Showtime s WHERE s.movie.id IN :movieIds AND s.deletedAt IS NULL GROUP BY s.movie.id")
    java.util.List<Object[]> countShowtimesByMovieIds(@org.springframework.data.repository.query.Param("movieIds") java.util.List<Long> movieIds);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"auditorium"})
    @org.springframework.data.jpa.repository.Query("""
            select s from Showtime s
            where s.cinema.id = :cinemaId
              and s.status = com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus.DRAFT
              and s.deletedAt is null
              and s.startTime >= :fromInclusive
              and (:toExclusive is null or s.startTime < :toExclusive)
            order by s.startTime asc
            """)
    org.springframework.data.domain.Page<Showtime> findFutureDraftsForPricingPolicy(
            @org.springframework.data.repository.query.Param("cinemaId") Long cinemaId,
            @org.springframework.data.repository.query.Param("fromInclusive") java.time.Instant fromInclusive,
            @org.springframework.data.repository.query.Param("toExclusive") java.time.Instant toExclusive,
            org.springframework.data.domain.Pageable pageable);
}
