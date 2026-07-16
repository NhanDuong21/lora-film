package com.lorafilm.movie.movie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import com.lorafilm.movie.common.enums.ActiveStatus;

public class PersonRequest {
    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]+$", message = "Full name contains invalid characters")
    private String fullName;

    @Size(max = 150)
    @jakarta.validation.constraints.Pattern(regexp = "^[^<>]*$", message = "Stage name contains invalid characters")
    private String stageName;

    private String biography;

    @jakarta.validation.constraints.Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    @Size(max = 100)
    private String nationality;

    @Size(max = 500)
    @org.hibernate.validator.constraints.URL(message = "Invalid URL format")
    @jakarta.validation.constraints.Pattern(regexp = "^(?i)(https?://).*\\.(jpg|jpeg|png|gif|webp|svg|bmp)([\\?#].*)?$", message = "URL must start with http/https and be a valid image file (.jpg, .png, etc.)")
    private String profileImageUrl;

    private ActiveStatus status;

    public PersonRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public ActiveStatus getStatus() { return status; }
    public void setStatus(ActiveStatus status) { this.status = status; }
}
