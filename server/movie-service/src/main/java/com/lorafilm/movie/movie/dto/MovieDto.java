package com.lorafilm.movie.movie.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;

import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public class MovieDto {

    private String publicId;
    private String slug;
    private String title;
    private String originalTitle;
    private String synopsis;
    private Integer durationMinutes;
    private AgeRating ageRating;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private List<String> genres;
    private String primaryPoster;
    private MovieStatus status;
    private String activeSlug;
    private String country;
    
    // Projection fields for Phase 4A
    private String source;
    private Long tmdbId;
    private java.time.LocalDateTime tmdbLastUpdated;
    private Long activeVersionCount;
    private Long mediaCount;
    private Long showtimeCount;
    private MovieReadinessDto readiness;
    private Boolean catalogVisible;
    private Boolean bookable;
    private Long bookableShowtimeCount;
    private Instant nextShowtimeAt;
    private BigDecimal priceFrom;
    private String currency;

    public MovieDto() {
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public AgeRating getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(AgeRating ageRating) {
        this.ageRating = ageRating;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getPrimaryPoster() {
        return primaryPoster;
    }

    public void setPrimaryPoster(String primaryPoster) {
        this.primaryPoster = primaryPoster;
    }

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public String getActiveSlug() {
        return activeSlug;
    }

    public void setActiveSlug(String activeSlug) {
        this.activeSlug = activeSlug;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }

    public java.time.LocalDateTime getTmdbLastUpdated() { return tmdbLastUpdated; }
    public void setTmdbLastUpdated(java.time.LocalDateTime tmdbLastUpdated) { this.tmdbLastUpdated = tmdbLastUpdated; }

    public Long getActiveVersionCount() { return activeVersionCount; }
    public void setActiveVersionCount(Long activeVersionCount) { this.activeVersionCount = activeVersionCount; }

    public Long getMediaCount() { return mediaCount; }
    public void setMediaCount(Long mediaCount) { this.mediaCount = mediaCount; }

    public Long getShowtimeCount() { return showtimeCount; }
    public void setShowtimeCount(Long showtimeCount) { this.showtimeCount = showtimeCount; }

    public MovieReadinessDto getReadiness() { return readiness; }
    public void setReadiness(MovieReadinessDto readiness) { this.readiness = readiness; }

    public Boolean getCatalogVisible() { return catalogVisible; }
    public void setCatalogVisible(Boolean catalogVisible) { this.catalogVisible = catalogVisible; }

    public Boolean getBookable() { return bookable; }
    public void setBookable(Boolean bookable) { this.bookable = bookable; }

    public Long getBookableShowtimeCount() { return bookableShowtimeCount; }
    public void setBookableShowtimeCount(Long bookableShowtimeCount) { this.bookableShowtimeCount = bookableShowtimeCount; }

    public Instant getNextShowtimeAt() { return nextShowtimeAt; }
    public void setNextShowtimeAt(Instant nextShowtimeAt) { this.nextShowtimeAt = nextShowtimeAt; }

    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
