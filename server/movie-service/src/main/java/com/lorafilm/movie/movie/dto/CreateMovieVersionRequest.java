package com.lorafilm.movie.movie.dto;

import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateMovieVersionRequest {

    @NotBlank(message = "Version name is required")
    @Size(max = 150, message = "Version name cannot exceed 150 characters")
    private String versionName;

    @NotNull(message = "Format is required")
    private MovieFormat format;

    @NotBlank(message = "Audio language is required")
    @Size(max = 50, message = "Audio language cannot exceed 50 characters")
    private String audioLanguage;

    @Size(max = 50, message = "Subtitle language cannot exceed 50 characters")
    private String subtitleLanguage;

    @Size(max = 50, message = "Dub language cannot exceed 50 characters")
    private String dubLanguage;

    private ActiveStatus status = ActiveStatus.ACTIVE;

    public CreateMovieVersionRequest() {}

    public CreateMovieVersionRequest(String versionName, MovieFormat format, String audioLanguage, String subtitleLanguage, String dubLanguage, ActiveStatus status) {
        this.versionName = versionName;
        this.format = format;
        this.audioLanguage = audioLanguage;
        this.subtitleLanguage = subtitleLanguage;
        this.dubLanguage = dubLanguage;
        this.status = status;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName != null ? versionName.trim() : null;
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
        this.audioLanguage = audioLanguage != null ? audioLanguage.trim() : null;
    }

    public String getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public void setSubtitleLanguage(String subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage != null ? subtitleLanguage.trim() : null;
    }

    public String getDubLanguage() {
        return dubLanguage;
    }

    public void setDubLanguage(String dubLanguage) {
        this.dubLanguage = dubLanguage != null ? dubLanguage.trim() : null;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }
}
