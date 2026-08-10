package com.lorafilm.movie.cinema.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lorafilm.movie.cinema.domain.entity.Cinema;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, Long>, JpaSpecificationExecutor<Cinema> {
    Optional<Cinema> findByPublicIdAndDeletedAtIsNull(String publicId);
    boolean existsByPublicIdAndDeletedAtIsNull(String publicId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select c from Cinema c where c.id = :cinemaId and c.deletedAt is null")
    Optional<Cinema> findByIdForScheduling(@Param("cinemaId") Long cinemaId);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.QueryHints(
            @jakarta.persistence.QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select c from Cinema c where c.publicId = :publicId and c.deletedAt is null")
    Optional<Cinema> findByPublicIdForScheduling(@Param("publicId") String publicId);
    Optional<Cinema> findByActiveSlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndDeletedAtIsNull(String slug);
    boolean existsBySlugAndPublicIdNotAndDeletedAtIsNull(String slug, String publicId);

    @Query("SELECT DISTINCT c FROM Cinema c JOIN CinemaClosurePeriod cp ON cp.cinema = c " +
            "WHERE c.status = 'ACTIVE' " +
            "AND c.deletedAt IS NULL " +
            "AND cp.status = 'ACTIVE' " +
            "AND cp.startTime <= :now AND cp.endTime >= :now")
    List<Cinema> findCinemasToClose(@Param("now") Instant now);

    @Query("SELECT c FROM Cinema c " +
            "WHERE c.status = 'TEMPORARILY_CLOSED' " +
            "AND c.deletedAt IS NULL " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM CinemaClosurePeriod cp " +
            "  WHERE cp.cinema = c " +
            "  AND cp.status = 'ACTIVE' " +
            "  AND cp.startTime <= :now AND cp.endTime >= :now" +
            ")")
    List<Cinema> findCinemasToOpen(@Param("now") Instant now);
}
