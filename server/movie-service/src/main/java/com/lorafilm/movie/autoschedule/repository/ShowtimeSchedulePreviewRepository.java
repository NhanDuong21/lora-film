package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSchedulePreviewRepository extends JpaRepository<ShowtimeSchedulePreview, Long>,
        ShowtimeSchedulePreviewHistoryRepository {

    Optional<ShowtimeSchedulePreview> findByPublicId(String publicId);

    @Query("""
        select p
        from ShowtimeSchedulePreview p
        join fetch p.cinema
        where p.publicId = :publicId
    """)
    Optional<ShowtimeSchedulePreview> findByPublicIdWithCinema(@Param("publicId") String publicId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update ShowtimeSchedulePreview p
        set p.selectedCandidateCount = :selectedCandidateCount,
            p.version = p.version + 1,
            p.updatedAt = :updatedAt
        where p.id = :previewId
          and p.status = :editableStatus
          and p.version = :expectedVersion
    """)
    int compareAndSetSelectionVersion(
            @Param("previewId") Long previewId,
            @Param("editableStatus") SchedulePreviewStatus editableStatus,
            @Param("expectedVersion") Long expectedVersion,
            @Param("selectedCandidateCount") Integer selectedCandidateCount,
            @Param("updatedAt") Instant updatedAt);

    Optional<ShowtimeSchedulePreview> findByGenerateIdempotencyKey(String generateIdempotencyKey);

    @Query("SELECT p FROM ShowtimeSchedulePreview p JOIN FETCH p.cinema WHERE p.generateIdempotencyKey = :generateIdempotencyKey")
    Optional<ShowtimeSchedulePreview> findByGenerateIdempotencyKeyWithCinema(@Param("generateIdempotencyKey") String generateIdempotencyKey);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from ShowtimeSchedulePreview p
        where p.publicId = :publicId
    """)
    Optional<ShowtimeSchedulePreview> findByPublicIdForExpiry(@Param("publicId") String publicId);

    List<ShowtimeSchedulePreview> findByStatusAndExpiresAtLessThanEqual(SchedulePreviewStatus status, Instant now);

    @Query("""
        select p
        from ShowtimeSchedulePreview p
        join fetch p.cinema
        where p.applyIdempotencyKey = :applyIdempotencyKey
    """)
    Optional<ShowtimeSchedulePreview> findByApplyIdempotencyKeyDetailed(
        @Param("applyIdempotencyKey") String applyIdempotencyKey
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from ShowtimeSchedulePreview p
        join fetch p.cinema
        where p.publicId = :publicId
    """)
    Optional<ShowtimeSchedulePreview> findByPublicIdForApply(
        @Param("publicId") String publicId
    );
}
