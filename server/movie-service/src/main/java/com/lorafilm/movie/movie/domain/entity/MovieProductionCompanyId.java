package com.lorafilm.movie.movie.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MovieProductionCompanyId implements Serializable {
    private Long movie;
    private Long productionCompany;
    private CompanyRoleType role;
}
