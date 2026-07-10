package com.lorafilm.movie.auditorium.domain.entity;

import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auditoriums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auditorium extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", updatable = false, unique = true, nullable = false)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "screen_type", nullable = false)
    private ScreenType screenType;

    @Enumerated(EnumType.STRING)
    @Column(name = "sound_type", nullable = false)
    private SoundType soundType;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "cleaning_buffer_minutes", nullable = false)
    private Integer cleaningBufferMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditoriumStatus status;
}
