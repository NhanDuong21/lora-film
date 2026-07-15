package com.lorafilm.movie.movie.dto;

import java.time.LocalDate;
import com.lorafilm.movie.common.enums.ActiveStatus;

public class PersonDto {
    private String publicId;
    private String fullName;
    private String stageName;
    private String biography;
    private LocalDate birthDate;
    private String nationality;
    private String profileImageUrl;
    private ActiveStatus status;

    public PersonDto() {}

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

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
