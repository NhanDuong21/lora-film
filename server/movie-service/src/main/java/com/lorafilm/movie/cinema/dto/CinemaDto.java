package com.lorafilm.movie.cinema.dto;

import java.math.BigDecimal;

public class CinemaDto {
    private String publicId;
    private String name;
    private String slug;
    private String city;
    private String district;
    private String address;
    private String hotline;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String status;

    public CinemaDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getHotline() { return hotline; }
    public void setHotline(String hotline) { this.hotline = hotline; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
