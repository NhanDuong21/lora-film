package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import java.time.Instant;

public class MovieVersionResponse {

    private String publicId;
    private String versionName;
    private MovieFormat format;
    private String audioLanguage;
    private String subtitleLanguage;
    private String dubLanguage;
    private ActiveStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public MovieVersionResponse() {}

    public MovieVersionResponse(String publicId, String versionName, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage, ActiveStatus status, Instant createdAt, Instant updatedAt) {
        this.publicId = publicId;
        this.versionName = versionName;
        this.format = format;
        this.audioLanguage = audioLanguage;
        this.subtitleLanguage = subtitleLanguage;
        this.dubLanguage = dubLanguage;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public MovieFormat getFormat() {
        return format;
    }

    public void setFormat(MovieFormat format) {
        this.format = format;
    }

    public String getAudioLanguage() {
        return audioLanguage;
    }

    public void setAudioLanguage(String audioLanguage) {
        this.audioLanguage = audioLanguage;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
    }

    public String getDubLanguage() {
        return dubLanguage;
    }

    public void setDubLanguage(String dubLanguage) {
        this.dubLanguage = dubLanguage;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
