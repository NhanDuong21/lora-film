package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class GenerateShowtimeSchedulePreviewRequest {

    @NotBlank
    @Size(max = 36)
    private String cinemaPublicId;

    private LocalDate scheduleFrom;

    private LocalDate scheduleTo;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> movieVersionPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> auditoriumPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> excludeMovieVersionPublicIds;

    @Size(max = 100)
    private List<@NotBlank @Size(max = 36) String> excludeAuditoriumPublicIds;

    @Min(5)
    @Max(60)
    private Integer slotGranularityMinutes;

    @Min(5)
    @Max(120)
    private Integer previewTtlMinutes;

    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;

    @Min(1)
    @Max(7)
    private Integer planningDays;

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public void setCinemaPublicId(String cinemaPublicId) {
        this.cinemaPublicId = cinemaPublicId;
    }

    public LocalDate getScheduleFrom() {
        return scheduleFrom;
    }

    public void setScheduleFrom(LocalDate scheduleFrom) {
        this.scheduleFrom = scheduleFrom;
    }

    public LocalDate getScheduleTo() {
        return scheduleTo;
    }

    public void setScheduleTo(LocalDate scheduleTo) {
        this.scheduleTo = scheduleTo;
    }

    public List<String> getMovieVersionPublicIds() {
        return movieVersionPublicIds;
    }

    public void setMovieVersionPublicIds(List<String> movieVersionPublicIds) {
        this.movieVersionPublicIds = movieVersionPublicIds;
    }

    public List<String> getAuditoriumPublicIds() {
        return auditoriumPublicIds;
    }

    public void setAuditoriumPublicIds(List<String> auditoriumPublicIds) {
        this.auditoriumPublicIds = auditoriumPublicIds;
    }

    public Integer getSlotGranularityMinutes() {
        return slotGranularityMinutes;
    }

    public void setSlotGranularityMinutes(Integer slotGranularityMinutes) {
        this.slotGranularityMinutes = slotGranularityMinutes;
    }

    public Integer getPreviewTtlMinutes() {
        return previewTtlMinutes;
    }

    public void setPreviewTtlMinutes(Integer previewTtlMinutes) {
        this.previewTtlMinutes = previewTtlMinutes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Integer getPlanningDays() { return planningDays; }
    public void setPlanningDays(Integer planningDays) { this.planningDays = planningDays; }
    public List<String> getExcludeMovieVersionPublicIds() { return excludeMovieVersionPublicIds; }
    public void setExcludeMovieVersionPublicIds(List<String> value) { this.excludeMovieVersionPublicIds = value; }
    public List<String> getExcludeAuditoriumPublicIds() { return excludeAuditoriumPublicIds; }
    public void setExcludeAuditoriumPublicIds(List<String> value) { this.excludeAuditoriumPublicIds = value; }
}
