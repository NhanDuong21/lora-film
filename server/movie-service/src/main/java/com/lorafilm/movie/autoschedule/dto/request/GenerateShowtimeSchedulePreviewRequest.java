package com.lorafilm.movie.autoschedule.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class GenerateShowtimeSchedulePreviewRequest {

    @NotBlank
    @Size(max = 36)
    private String cinemaPublicId;

    @NotNull
    private LocalDate scheduleFrom;

    @NotNull
    private LocalDate scheduleTo;

    @NotEmpty
    @Size(max = 20)
    private List<@NotBlank @Size(max = 36) String> movieVersionPublicIds;

    @NotEmpty
    @Size(max = 20)
    private List<@NotBlank @Size(max = 36) String> auditoriumPublicIds;

    @NotNull
    @Min(5)
    @Max(60)
    private Integer slotGranularityMinutes;

    @NotNull
    @Min(5)
    @Max(120)
    private Integer previewTtlMinutes;

    @NotBlank
    @Size(max = 100)
    private String idempotencyKey;

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
}
