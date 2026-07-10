package com.lorafilm.movie.movie.domain.entity;

import java.io.Serializable;
import java.util.Objects;
import com.lorafilm.movie.movie.domain.enums.CompanyRoleType;

public class MovieProductionCompanyId implements java.io.Serializable {

    private Long movie;

    private Long productionCompany;

    private CompanyRoleType role;

    public MovieProductionCompanyId() {}

    public Long getMovie() {
        return movie;
    }

    public void setMovie(Long movie) {
        this.movie = movie;
    }

    public Long getProductionCompany() {
        return productionCompany;
    }

    public void setProductionCompany(Long productionCompany) {
        this.productionCompany = productionCompany;
    }

    public CompanyRoleType getRole() {
        return role;
    }

    public void setRole(CompanyRoleType role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MovieProductionCompanyId that = (MovieProductionCompanyId) o;
        return Objects.equals(movie, that.movie) && Objects.equals(productionCompany, that.productionCompany) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(movie, productionCompany, role);
    }
}
