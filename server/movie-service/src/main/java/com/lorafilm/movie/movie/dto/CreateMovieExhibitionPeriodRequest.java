package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateMovieExhibitionPeriodRequest {

    @NotNull(message = "Vui lòng chọn ngày bắt đầu khai thác.")
    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 500, message = "Ghi chú không được dài quá 500 ký tự.")
    private String note;

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? null : note.trim(); }
}
