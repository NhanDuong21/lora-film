package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.lorafilm.movie.movie.domain.enums.CompanyRoleType;

public class MovieCompanyRequest {
    @NotBlank(message = "Company public ID is required")
    private String companyPublicId;

    @NotNull(message = "Role is required")
    private CompanyRoleType role;

    public MovieCompanyRequest() {}

    public String getCompanyPublicId() { return companyPublicId; }
    public void setCompanyPublicId(String companyPublicId) { this.companyPublicId = companyPublicId; }

    public CompanyRoleType getRole() { return role; }
    public void setRole(CompanyRoleType role) { this.role = role; }
}
