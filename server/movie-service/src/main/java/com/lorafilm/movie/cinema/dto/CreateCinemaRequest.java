package com.lorafilm.movie.cinema.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateCinemaRequest {

    @NotBlank(message = "Cinema name is required")
    @Size(max = 150, message = "Cinema name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;

    @Size(max = 30, message = "Hotline cannot exceed 30 characters")
    private String hotline;

    private String description;
    private LocalDate openedDate;
    private LocalDate closedDate;

    public CreateCinemaRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getHotline() {
        return hotline;
    }

    public void setHotline(String hotline) {
        this.hotline = hotline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getOpenedDate() {
        return openedDate;
    }

    public void setOpenedDate(LocalDate openedDate) {
        this.openedDate = openedDate;
    }

    public LocalDate getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(LocalDate closedDate) {
        this.closedDate = closedDate;
    }
}
