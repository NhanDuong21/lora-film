package com.lorafilm.movie.movie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MovieCreditAssignRequest {
    @NotEmpty(message = "Danh sách nhân sự của phim không được để trống.")
    @Valid
    private List<MovieCreditRequest> credits;

    public MovieCreditAssignRequest() {}

    public List<MovieCreditRequest> getCredits() {
        return credits;
    }

    public void setCredits(List<MovieCreditRequest> credits) {
        this.credits = credits;
    }
}
