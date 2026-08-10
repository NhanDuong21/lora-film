package com.lorafilm.movie.movie.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "people")
public class Person extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false, columnDefinition = "CHAR(36)")
    private String publicId;

    @Column(name = "tmdb_id", unique = true)
    private Long tmdbPersonId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "stage_name")
    private String stageName;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    public Person() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public Long getTmdbPersonId() {
        return tmdbPersonId;
    }

    public void setTmdbPersonId(Long tmdbPersonId) {
        this.tmdbPersonId = tmdbPersonId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }
}
