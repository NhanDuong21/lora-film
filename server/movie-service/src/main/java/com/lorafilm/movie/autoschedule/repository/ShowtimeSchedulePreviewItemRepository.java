package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeSchedulePreviewItemRepository extends JpaRepository<ShowtimeSchedulePreviewItem, Long> {

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
}
