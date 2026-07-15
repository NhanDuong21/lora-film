package com.lorafilm.movie.autoschedule.model;

import java.time.LocalDate;
import java.util.List;

public class NormalizedGeneratePreviewRequest {
    private final String cinemaPublicId;
    private final LocalDate scheduleFrom;
    private final LocalDate scheduleTo;
    private final List<String> movieVersionPublicIds;
    private final List<String> auditoriumPublicIds;
    private final Integer slotGranularityMinutes;
    private final Integer previewTtlMinutes;
    private final String idempotencyKey;

    public NormalizedGeneratePreviewRequest(String cinemaPublicId, LocalDate scheduleFrom, LocalDate scheduleTo,
                                            List<String> movieVersionPublicIds, List<String> auditoriumPublicIds,
                                            Integer slotGranularityMinutes, Integer previewTtlMinutes, String idempotencyKey) {
        this.cinemaPublicId = cinemaPublicId;
        this.scheduleFrom = scheduleFrom;
        this.scheduleTo = scheduleTo;
        this.movieVersionPublicIds = movieVersionPublicIds;
        this.auditoriumPublicIds = auditoriumPublicIds;
        this.slotGranularityMinutes = slotGranularityMinutes;
        this.previewTtlMinutes = previewTtlMinutes;
        this.idempotencyKey = idempotencyKey;
    }

    public String getCinemaPublicId() {
        return cinemaPublicId;
    }

    public LocalDate getScheduleFrom() {
        return scheduleFrom;
    }

    public LocalDate getScheduleTo() {
        return scheduleTo;
    }

    public List<String> getMovieVersionPublicIds() {
        return movieVersionPublicIds;
    }

    public List<String> getAuditoriumPublicIds() {
        return auditoriumPublicIds;
    }

    public Integer getSlotGranularityMinutes() {
        return slotGranularityMinutes;
    }

    public Integer getPreviewTtlMinutes() {
        return previewTtlMinutes;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
