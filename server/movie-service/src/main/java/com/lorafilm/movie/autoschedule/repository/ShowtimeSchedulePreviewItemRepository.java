package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSchedulePreviewItemRepository extends JpaRepository<ShowtimeSchedulePreviewItem, Long>, JpaSpecificationExecutor<ShowtimeSchedulePreviewItem> {

    @EntityGraph(attributePaths = {
            "movie",
            "movieVersion",
            "cinema",
            "auditorium",
            "createdShowtime"
    })
    Page<ShowtimeSchedulePreviewItem> findAll(Specification<ShowtimeSchedulePreviewItem> spec, Pageable pageable);

    Optional<ShowtimeSchedulePreviewItem> findByPublicId(String publicId);

    List<ShowtimeSchedulePreviewItem> findAllByPreviewIdOrderByRankingPositionAscIdAsc(Long previewId);

    List<ShowtimeSchedulePreviewItem> findAllByPreviewIdAndSelectedTrueAndValidationStatusAndApplyStatus(
            Long previewId,
            PreviewItemValidationStatus validationStatus,
            PreviewItemApplyStatus applyStatus
    );

    @Query("""
        select i
        from ShowtimeSchedulePreviewItem i
        join fetch i.movie
        join fetch i.movieVersion
        join fetch i.cinema
        join fetch i.auditorium
        left join fetch i.createdShowtime
        where i.preview.id = :previewId
        order by i.rankingPosition asc, i.id asc
    """)
    List<ShowtimeSchedulePreviewItem> findDetailedItemsByPreviewId(@Param("previewId") Long previewId);

    List<ShowtimeSchedulePreviewItem> findAllByPublicIdIn(java.util.Collection<String> publicIds);

    @Query("""
        select new com.lorafilm.movie.autoschedule.repository.SelectionItemSnapshot(
               i.id, i.publicId, i.preview.id, a.id,
               i.startTime, i.endTime, i.occupancyEndTime,
               i.validationStatus, i.selected, i.applyStatus)
        from ShowtimeSchedulePreviewItem i
        left join i.auditorium a
        where i.publicId in :publicIds
    """)
    List<SelectionItemSnapshot> findSelectionSnapshotsByPublicIdIn(
            @Param("publicIds") Collection<String> publicIds);

    @Query("""
        select new com.lorafilm.movie.autoschedule.repository.SelectionItemSnapshot(
               i.id, i.publicId, i.preview.id, a.id,
               i.startTime, i.endTime, i.occupancyEndTime,
               i.validationStatus, i.selected, i.applyStatus)
        from ShowtimeSchedulePreviewItem i
        left join i.auditorium a
        where i.preview.id = :previewId
          and i.selected = true
    """)
    List<SelectionItemSnapshot> findSelectedSelectionSnapshots(
            @Param("previewId") Long previewId);

    @Modifying
    @Query("""
        update ShowtimeSchedulePreviewItem i
        set i.selected = :selected,
            i.selectedAt = :selectedAt,
            i.selectedBy = :selectedBy,
            i.updatedAt = :selectedAt
        where i.preview.id = :previewId
          and i.id in :itemIds
          and i.selected = :expectedSelected
    """)
    int updateSelectionState(
            @Param("previewId") Long previewId,
            @Param("itemIds") Collection<Long> itemIds,
            @Param("expectedSelected") Boolean expectedSelected,
            @Param("selected") Boolean selected,
            @Param("selectedAt") Instant selectedAt,
            @Param("selectedBy") Long selectedBy);

    long countByPreviewIdAndSelectedTrueAndValidationStatus(Long previewId, PreviewItemValidationStatus validationStatus);

    @Query("""
        select i
        from ShowtimeSchedulePreviewItem i
        join fetch i.movie
        join fetch i.movieVersion
        join fetch i.cinema
        join fetch i.auditorium
        where i.preview.id = :previewId
          and i.selected = true
          and i.validationStatus = :validStatus
        order by i.auditorium.id asc,
                 i.startTime asc,
                 i.rankingPosition asc,
                 i.id asc
    """)
    List<ShowtimeSchedulePreviewItem> findSelectedItemsForApply(
        @Param("previewId") Long previewId,
        @Param("validStatus") PreviewItemValidationStatus validStatus
    );

    @Query("""
        select i.id as itemId,
               i.publicId as itemPublicId,
               i.movie.id as movieId,
               i.movieVersion.id as movieVersionId,
               i.cinema.id as cinemaId,
               i.auditorium.id as auditoriumId
        from ShowtimeSchedulePreviewItem i
        where i.preview.id = :previewId
          and i.selected = true
          and i.validationStatus = :validStatus
        order by i.auditorium.id asc,
                 i.startTime asc,
                 i.rankingPosition asc,
                 i.id asc
    """)
    List<ApplyItemReference> findSelectedItemReferencesForApply(
            @Param("previewId") Long previewId,
            @Param("validStatus") PreviewItemValidationStatus validStatus
    );

    interface ApplyItemReference {
        Long getItemId();
        String getItemPublicId();
        Long getMovieId();
        Long getMovieVersionId();
        Long getCinemaId();
        Long getAuditoriumId();
    }
}
