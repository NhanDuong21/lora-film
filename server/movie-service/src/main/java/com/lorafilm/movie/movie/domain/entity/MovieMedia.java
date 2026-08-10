package com.lorafilm.movie.movie.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.common.enums.ActiveStatus;
import com.lorafilm.movie.movie.domain.enums.MovieMediaType;
import jakarta.persistence.*;

@Entity
@Table(name = "movie_media")
public class MovieMedia extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false, columnDefinition = "CHAR(36)")
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MovieMediaType mediaType;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "title")
    private String title;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActiveStatus status = ActiveStatus.ACTIVE;

    public MovieMedia() {}

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

    public MovieMediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MovieMediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public ActiveStatus getStatus() {
        return status;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }
}
