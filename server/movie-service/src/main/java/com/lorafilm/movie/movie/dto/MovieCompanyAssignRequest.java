package com.lorafilm.movie.movie.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class MovieCompanyAssignRequest {
    @NotEmpty(message = "Companies cannot be empty")
    @Valid
    private List<MovieCompanyRequest> companies;

    public MovieCompanyAssignRequest() {}

    public List<MovieCompanyRequest> getCompanies() {
        return companies;
    }

    public void setCompanies(List<MovieCompanyRequest> companies) {
        this.companies = companies;
    }
}
