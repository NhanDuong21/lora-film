package com.lorafilm.movie.autoschedule.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewItem;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewItemResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewResponse;
import com.lorafilm.movie.autoschedule.dto.response.ShowtimeSchedulePreviewSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ShowtimeSchedulePreviewMapper {

    private static final Logger log = LoggerFactory.getLogger(ShowtimeSchedulePreviewMapper.class);

    private final ObjectMapper objectMapper;

    public ShowtimeSchedulePreviewMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ShowtimeSchedulePreviewResponse toResponse(ShowtimeSchedulePreview preview, List<ShowtimeSchedulePreviewItem> items) {
        if (preview == null) {
            return null;
        }

        ShowtimeSchedulePreviewResponse response = new ShowtimeSchedulePreviewResponse();
        response.setPreviewPublicId(preview.getPublicId());
        response.setVersion(preview.getVersion());
        response.setCinemaPublicId(preview.getCinema() != null ? preview.getCinema().getPublicId() : null);
        response.setCinemaName(preview.getCinema() != null ? preview.getCinema().getName() : null);
        response.setScheduleFrom(preview.getScheduleFrom());
        response.setScheduleTo(preview.getScheduleTo());
        response.setTimezoneSnapshot(preview.getTimezoneSnapshot());
        response.setStrategy(preview.getStrategy());
        response.setStrategyVersion(preview.getStrategyVersion());
        response.setApplyMode(preview.getApplyMode());
        response.setStatus(preview.getStatus());
        response.setSlotGranularityMinutes(preview.getSlotGranularityMinutes());
        response.setSummary(toSummary(preview));
        response.setGeneratedAt(preview.getGeneratedAt());
        response.setExpiresAt(preview.getExpiresAt());
        response.setGeneratedBy(preview.getGeneratedBy());
        response.setAppliedAt(preview.getAppliedAt());
        response.setAppliedBy(preview.getAppliedBy());
        response.setFailureReason(preview.getFailureReason());
        response.setItems(items != null ? items.stream().map(this::toItemResponse).toList() : Collections.emptyList());
        
        return response;
    }

    public ShowtimeSchedulePreviewItemResponse toItemResponse(ShowtimeSchedulePreviewItem item) {
        if (item == null) {
            return null;
        }

        ShowtimeSchedulePreviewItemResponse response = new ShowtimeSchedulePreviewItemResponse();
        response.setItemPublicId(item.getPublicId());
        response.setMoviePublicId(item.getMovie() != null ? item.getMovie().getPublicId() : null);
        response.setMovieTitle(item.getMovie() != null ? item.getMovie().getTitle() : null);
        response.setMovieSlug(item.getMovie() != null ? item.getMovie().getSlug() : null);
        response.setMovieVersionPublicId(item.getMovieVersion() != null ? item.getMovieVersion().getPublicId() : null);
        response.setVersionName(item.getMovieVersion() != null ? item.getMovieVersion().getVersionName() : null);
        response.setFormat(item.getMovieVersion() != null && item.getMovieVersion().getFormat() != null ? item.getMovieVersion().getFormat().name() : null);
        response.setAudioLanguage(item.getMovieVersion() != null ? item.getMovieVersion().getAudioLanguage() : null);
        response.setSubtitleLanguage(item.getMovieVersion() != null ? item.getMovieVersion().getSubtitleLanguage() : null);
        response.setDubLanguage(item.getMovieVersion() != null ? item.getMovieVersion().getDubLanguage() : null);
        response.setCinemaPublicId(item.getCinema() != null ? item.getCinema().getPublicId() : null);
        response.setCinemaName(item.getCinema() != null ? item.getCinema().getName() : null);
        response.setAuditoriumPublicId(item.getAuditorium() != null ? item.getAuditorium().getPublicId() : null);
        response.setAuditoriumName(item.getAuditorium() != null ? item.getAuditorium().getName() : null);
        response.setScreenType(item.getAuditorium() != null && item.getAuditorium().getScreenType() != null ? item.getAuditorium().getScreenType().name() : null);
        response.setSoundType(item.getAuditorium() != null && item.getAuditorium().getSoundType() != null ? item.getAuditorium().getSoundType().name() : null);
        response.setStartTime(item.getStartTime());
        response.setEndTime(item.getEndTime());
        response.setOccupancyEndTime(item.getOccupancyEndTime());
        response.setScore(item.getScore());
        response.setScoreBreakdown(item.getScoreBreakdown());
        response.setRankingPosition(item.getRankingPosition());
        response.setValidationStatus(item.getValidationStatus());
        response.setRejectionCode(item.getRejectionCode());
        response.setRejectionReason(item.getRejectionReason());
        response.setSelected(item.getSelected());
        response.setSelectedAt(item.getSelectedAt());
        response.setSelectedBy(item.getSelectedBy());
        response.setApplyStatus(item.getApplyStatus());
        response.setCreatedShowtimePublicId(item.getCreatedShowtime() != null ? item.getCreatedShowtime().getPublicId() : null);
        response.setApplyErrorCode(item.getApplyErrorCode());
        response.setApplyErrorMessage(item.getApplyErrorMessage());
        
        return response;
    }

    public ShowtimeSchedulePreviewSummaryResponse toSummary(ShowtimeSchedulePreview preview) {
        if (preview == null) {
            return null;
        }
        
        ShowtimeSchedulePreviewSummaryResponse summary = new ShowtimeSchedulePreviewSummaryResponse();
        summary.setTotalCandidateCount(preview.getTotalCandidateCount());
        summary.setValidCandidateCount(preview.getValidCandidateCount());
        summary.setRejectedCandidateCount(preview.getRejectedCandidateCount());
        summary.setSelectedCandidateCount(preview.getSelectedCandidateCount());
        return summary;
    }

}
