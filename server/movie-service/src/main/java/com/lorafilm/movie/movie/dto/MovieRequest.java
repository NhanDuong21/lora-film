package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class MovieRequest {
    @NotBlank(message = "Title is required")
    @jakarta.validation.constraints.Size(min = 1, max = 255)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]+$", message = "Title contains invalid characters")
    private String title;
    
    @jakarta.validation.constraints.Size(max = 255)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]*$", message = "Original title contains invalid characters")
    private String originalTitle;
    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;
    @NotNull(message = "Age rating is required")
    private AgeRating ageRating;
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;
    private LocalDate endDate;
    private String country;
    private String synopsis;

    public MovieRequest() {}
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title.trim() : null; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle != null ? originalTitle.trim() : null; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis != null ? synopsis.trim() : null; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public AgeRating getAgeRating() { return ageRating; }
    public void setAgeRating(AgeRating ageRating) { this.ageRating = ageRating; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country != null ? country.trim() : null; }
}
