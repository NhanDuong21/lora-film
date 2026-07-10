package com.lorafilm.movie.movie.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "movie_production_companies")
@IdClass(MovieProductionCompanyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private CompanyRoleType role;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
