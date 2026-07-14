package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSchedulePreviewRepository extends JpaRepository<ShowtimeSchedulePreview, Long> {

    Optional<ShowtimeSchedulePreview> findByPublicId(String publicId);

    Optional<ShowtimeSchedulePreview> findByGenerateIdempotencyKey(String generateIdempotencyKey);

    Optional<ShowtimeSchedulePreview> findByApplyIdempotencyKey(String applyIdempotencyKey);

    boolean existsByGenerateIdempotencyKey(String generateIdempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "0"
        )
    )
    @Query("""
        select p
        from ShowtimeSchedulePreview p
        where p.publicId = :publicId
    """)
    Optional<ShowtimeSchedulePreview> findByPublicIdForUpdate(@Param("publicId") String publicId);

    List<ShowtimeSchedulePreview> findByStatusAndExpiresAtLessThanEqual(SchedulePreviewStatus status, Instant now);
}
