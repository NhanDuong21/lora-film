package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.lorafilm.movie.common.enums.ActiveStatus;

public class ProductionCompanyRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 180)
    @jakarta.validation.constraints.Pattern(regexp = "^[\\p{L}0-9\\s&\\-\\.,']+$", message = "Name contains invalid characters")
    private String name;

    @Size(max = 100)
    private String country;

    @Size(max = 500)
    @org.hibernate.validator.constraints.URL(message = "Invalid URL format")
    @jakarta.validation.constraints.Pattern(regexp = "^(?i)(https?://).*\\.(jpg|jpeg|png|gif|webp|svg|bmp)([\\?#].*)?$", message = "URL must start with http/https and be a valid image file (.jpg, .png, etc.)")
    private String logoUrl;

    private ActiveStatus status;

    public ProductionCompanyRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
