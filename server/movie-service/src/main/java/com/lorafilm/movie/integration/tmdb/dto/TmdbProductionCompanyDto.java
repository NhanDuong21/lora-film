package com.lorafilm.movie.integration.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbProductionCompanyDto {
    private Long tmdbCompanyId;
    private String name;
    private String logoUrl;
    private String originCountry;

    public Long getTmdbCompanyId() {
        return tmdbCompanyId;
    }

    public void setTmdbCompanyId(Long tmdbCompanyId) {
        this.tmdbCompanyId = tmdbCompanyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }
}
