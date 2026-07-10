package com.lorafilm.movie.movie.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import com.lorafilm.movie.movie.domain.enums.CompanyRoleType;

@IdClass(MovieProductionCompanyId.class)
@Entity
@Table(name = "movie_production_companies")
public class MovieProductionCompany {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_company_id", nullable = false)
    private ProductionCompany productionCompany;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private CompanyRoleType role = CompanyRoleType.PRODUCTION;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    public MovieProductionCompany() {}

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public ProductionCompany getProductionCompany() {
        return productionCompany;
    }

    public void setProductionCompany(ProductionCompany productionCompany) {
        this.productionCompany = productionCompany;
    }

    public CompanyRoleType getRole() {
        return role;
    }

    public void setRole(CompanyRoleType role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
