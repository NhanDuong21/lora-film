package com.lorafilm.movie.autoschedule.dto.request;

import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemApplyStatus;
import com.lorafilm.movie.autoschedule.domain.enums.PreviewItemValidationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class ShowtimeSchedulePreviewItemQuery {

    @Min(0)
    private int page = 0;

    @Min(1)
    @Max(100)
    private int size = 50;

    private Boolean selected;

    private PreviewItemValidationStatus validationStatus;

    private PreviewItemApplyStatus applyStatus;

    private String auditoriumPublicId;

    private String movieVersionPublicId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private String sort = "rankingPosition,asc";

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public PreviewItemValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(PreviewItemValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public PreviewItemApplyStatus getApplyStatus() {
        return applyStatus;
    }

    public void setApplyStatus(PreviewItemApplyStatus applyStatus) {
        this.applyStatus = applyStatus;
    }

    public String getAuditoriumPublicId() {
        return auditoriumPublicId;
    }

    public void setAuditoriumPublicId(String auditoriumPublicId) {
        this.auditoriumPublicId = auditoriumPublicId;
    }

    public String getMovieVersionPublicId() {
        return movieVersionPublicId;
    }

    public void setMovieVersionPublicId(String movieVersionPublicId) {
        this.movieVersionPublicId = movieVersionPublicId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
