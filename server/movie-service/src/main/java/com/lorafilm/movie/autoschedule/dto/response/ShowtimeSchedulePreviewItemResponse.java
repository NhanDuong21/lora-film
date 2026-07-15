package com.lorafilm.movie.autoschedule.dto.response;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public class ShowtimeSchedulePreviewItemResponse {

    private String itemPublicId;

    private String moviePublicId;
    private String movieTitle;
    private String movieSlug;

    private String movieVersionPublicId;
    private String versionName;
    private String format;
    private String audioLanguage;
    private String subtitleLanguage;
    private String dubLanguage;

    private String cinemaPublicId;
    private String cinemaName;

    private String auditoriumPublicId;
    private String auditoriumName;
    private String screenType;
    private String soundType;

    private Instant startTime;
    private Instant endTime;
    private Instant occupancyEndTime;

    private BigDecimal score;
    private Map<String, BigDecimal> scoreBreakdown;

    private Integer rankingPosition;

    private PreviewItemValidationStatus validationStatus;
    private String rejectionCode;
    private String rejectionReason;

    private Boolean selected;
    private Instant selectedAt;
    private Long selectedBy;

    private PreviewItemApplyStatus applyStatus;

    private String createdShowtimePublicId;
    private String applyErrorCode;
    private String applyErrorMessage;

    public ShowtimeSchedulePreviewItemResponse() {
    }

    public String getItemPublicId() { return itemPublicId; }
    public void setItemPublicId(String itemPublicId) { this.itemPublicId = itemPublicId; }

    public String getMoviePublicId() { return moviePublicId; }
    public void setMoviePublicId(String moviePublicId) { this.moviePublicId = moviePublicId; }

    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

    public String getMovieSlug() { return movieSlug; }
    public void setMovieSlug(String movieSlug) { this.movieSlug = movieSlug; }

    public String getMovieVersionPublicId() { return movieVersionPublicId; }
    public void setMovieVersionPublicId(String movieVersionPublicId) { this.movieVersionPublicId = movieVersionPublicId; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getAudioLanguage() { return audioLanguage; }
    public void setAudioLanguage(String audioLanguage) { this.audioLanguage = audioLanguage; }

    public String getSubtitleLanguage() { return subtitleLanguage; }
    public void setSubtitleLanguage(String subtitleLanguage) { this.subtitleLanguage = subtitleLanguage; }

    public String getDubLanguage() { return dubLanguage; }
    public void setDubLanguage(String dubLanguage) { this.dubLanguage = dubLanguage; }

    public String getCinemaPublicId() { return cinemaPublicId; }
    public void setCinemaPublicId(String cinemaPublicId) { this.cinemaPublicId = cinemaPublicId; }

    public String getCinemaName() { return cinemaName; }
    public void setCinemaName(String cinemaName) { this.cinemaName = cinemaName; }

    public String getAuditoriumPublicId() { return auditoriumPublicId; }
    public void setAuditoriumPublicId(String auditoriumPublicId) { this.auditoriumPublicId = auditoriumPublicId; }

    public String getAuditoriumName() { return auditoriumName; }
    public void setAuditoriumName(String auditoriumName) { this.auditoriumName = auditoriumName; }

    public String getScreenType() { return screenType; }
    public void setScreenType(String screenType) { this.screenType = screenType; }

    public String getSoundType() { return soundType; }
    public void setSoundType(String soundType) { this.soundType = soundType; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public Instant getOccupancyEndTime() { return occupancyEndTime; }
    public void setOccupancyEndTime(Instant occupancyEndTime) { this.occupancyEndTime = occupancyEndTime; }

    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }

    public Map<String, BigDecimal> getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(Map<String, BigDecimal> scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public Integer getRankingPosition() { return rankingPosition; }
    public void setRankingPosition(Integer rankingPosition) { this.rankingPosition = rankingPosition; }

    public PreviewItemValidationStatus getValidationStatus() { return validationStatus; }
    public void setValidationStatus(PreviewItemValidationStatus validationStatus) { this.validationStatus = validationStatus; }

    public String getRejectionCode() { return rejectionCode; }
    public void setRejectionCode(String rejectionCode) { this.rejectionCode = rejectionCode; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public Boolean getSelected() { return selected; }
    public void setSelected(Boolean selected) { this.selected = selected; }

    public Instant getSelectedAt() { return selectedAt; }
    public void setSelectedAt(Instant selectedAt) { this.selectedAt = selectedAt; }

    public Long getSelectedBy() { return selectedBy; }
    public void setSelectedBy(Long selectedBy) { this.selectedBy = selectedBy; }

    public PreviewItemApplyStatus getApplyStatus() { return applyStatus; }
    public void setApplyStatus(PreviewItemApplyStatus applyStatus) { this.applyStatus = applyStatus; }

    public String getCreatedShowtimePublicId() { return createdShowtimePublicId; }
    public void setCreatedShowtimePublicId(String createdShowtimePublicId) { this.createdShowtimePublicId = createdShowtimePublicId; }

    public String getApplyErrorCode() { return applyErrorCode; }
    public void setApplyErrorCode(String applyErrorCode) { this.applyErrorCode = applyErrorCode; }

    public String getApplyErrorMessage() { return applyErrorMessage; }
    public void setApplyErrorMessage(String applyErrorMessage) { this.applyErrorMessage = applyErrorMessage; }
}
