package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.movie.domain.enums.AgeRating;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public class MovieRequest {
    @NotBlank(message = "Vui lòng nhập tên phim.")
    @jakarta.validation.constraints.Size(min = 1, max = 255)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]+$", message = "Tên phim chứa ký tự không hợp lệ.")
    private String title;
    
    @jakarta.validation.constraints.Size(max = 255)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]*$", message = "Tên gốc chứa ký tự không hợp lệ.")
    private String originalTitle;
    @NotNull(message = "Vui lòng nhập thời lượng phim.")
    @Positive(message = "Thời lượng phim phải lớn hơn 0 phút.")
    private Integer durationMinutes;
    @NotNull(message = "Vui lòng chọn phân loại độ tuổi.")
    private AgeRating ageRating;
    private LocalDate originalReleaseDate;
    @NotNull(message = "Vui lòng chọn ngày bắt đầu khai thác tại rạp.")
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
    public LocalDate getOriginalReleaseDate() { return originalReleaseDate; }
    public void setOriginalReleaseDate(LocalDate originalReleaseDate) { this.originalReleaseDate = originalReleaseDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country != null ? country.trim() : null; }
}
