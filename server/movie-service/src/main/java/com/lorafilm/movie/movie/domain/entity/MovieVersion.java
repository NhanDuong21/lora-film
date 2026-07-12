package com.lorafilm.movie.movie.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieFormat;
import jakarta.persistence.*;

@Entity
@Table(name = "movie_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_movie_version_unique", columnNames = {"movie_id", "format", "audio_language", "subtitle_language", "dub_language"})
})
public class MovieVersion extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false, columnDefinition = "CHAR(36)")
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(name = "version_name", nullable = false)
    private String versionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false)
    private MovieFormat format;

    @Column(name = "audio_language", nullable = false)
    private String audioLanguage;

    @Column(name = "subtitle_language")
    private String subtitleLanguage;

    @Column(name = "dub_language")
    private String dubLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    public MovieVersion() {
    }

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

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
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
}
