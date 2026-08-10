package com.lorafilm.movie.autoschedule.dto.response;

import java.util.List;
import java.time.LocalDate;
import com.lorafilm.movie.movie.dto.MovieVersionResponse;
import com.lorafilm.movie.movie.domain.enums.MovieStatus;

public class EligibleMovieResponse {
    private String moviePublicId;
    private String title;
    private String originalTitle;
    private String slug;
    private String primaryPoster;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private LocalDate endDate;
    private MovieStatus status;
    private boolean eligible;
    private List<EligibilityReason> reasons;
    private List<MovieVersionResponse> versions;

    public EligibleMovieResponse() {}

    public String getMoviePublicId() {
        return moviePublicId;
    }

    public void setMoviePublicId(String moviePublicId) {
        this.moviePublicId = moviePublicId;
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

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPrimaryPoster() {
        return primaryPoster;
    }

    public void setPrimaryPoster(String primaryPoster) {
        this.primaryPoster = primaryPoster;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
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

    public MovieStatus getStatus() {
        return status;
    }

    public void setStatus(MovieStatus status) {
        this.status = status;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<EligibilityReason> getReasons() {
        return reasons;
    }

    public void setReasons(List<EligibilityReason> reasons) {
        this.reasons = reasons;
    }

    public List<MovieVersionResponse> getVersions() {
        return versions;
    }

    public void setVersions(List<MovieVersionResponse> versions) {
        this.versions = versions;
    }
}
