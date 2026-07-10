package com.lorafilm.movie.movie.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.common.enums.ActiveStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movie_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieVersion extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false)
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
    private ActiveStatus status;
}
