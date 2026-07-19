package com.lorafilm.movie.autoschedule.mapper;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.dto.response.AppliedShowtimeResponse;
import com.lorafilm.movie.autoschedule.dto.response.ApplyShowtimeSchedulePreviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AutoScheduleApplyResponseMapper {

    public ApplyShowtimeSchedulePreviewResponse toResponse(ShowtimeSchedulePreview preview) {
        ApplyShowtimeSchedulePreviewResponse response = new ApplyShowtimeSchedulePreviewResponse();
        response.setPreviewPublicId(preview.getPublicId());
        response.setVersion(preview.getVersion());
        response.setStatus(preview.getStatus());
        
        response.setCinemaPublicId(preview.getCinema().getPublicId());
        response.setCinemaName(preview.getCinema().getName());
        
        response.setAppliedAt(preview.getAppliedAt());
        response.setAppliedBy(preview.getAppliedBy());

        List<ShowtimeSchedulePreviewItem> items = preview.getItems();
        
        int createdCount = (int) items.stream()
                .filter(i -> i.getApplyStatus() == PreviewItemApplyStatus.CREATED)
                .count();
                
        int skippedCount = (int) items.stream()
                .filter(i -> i.getApplyStatus() == PreviewItemApplyStatus.SKIPPED)
                .count();
                
        response.setCreatedShowtimeCount(createdCount);
        response.setSkippedItemCount(skippedCount);

        List<AppliedShowtimeResponse> showtimeResponses = items.stream()
                .filter(i -> i.getApplyStatus() == PreviewItemApplyStatus.CREATED && i.getCreatedShowtime() != null)
                .map(this::toAppliedShowtimeResponse)
                .collect(Collectors.toList());

        response.setCreatedShowtimes(showtimeResponses);
        return response;
    }

    private AppliedShowtimeResponse toAppliedShowtimeResponse(ShowtimeSchedulePreviewItem item) {
        AppliedShowtimeResponse response = new AppliedShowtimeResponse();
        response.setPreviewItemPublicId(item.getPublicId());
        response.setShowtimePublicId(item.getCreatedShowtime().getPublicId());
        
        response.setMoviePublicId(item.getMovie().getPublicId());
        response.setMovieTitle(item.getMovie().getTitle());
        response.setMovieVersionPublicId(item.getMovieVersion().getPublicId());
        
        response.setAuditoriumPublicId(item.getAuditorium().getPublicId());
        response.setAuditoriumName(item.getAuditorium().getName());
        
        response.setStartTime(item.getCreatedShowtime().getStartTime());
        response.setEndTime(item.getCreatedShowtime().getEndTime());
        response.setStatus(item.getCreatedShowtime().getStatus().name());
        
        return response;
    }
}
