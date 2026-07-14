package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;

public class ProductionCompanyDto {
    private String publicId;
    private String name;
    private String country;
    private String logoUrl;
    private ActiveStatus status;

    public ProductionCompanyDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
