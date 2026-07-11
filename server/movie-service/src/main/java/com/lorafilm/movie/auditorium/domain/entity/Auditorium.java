package com.lorafilm.movie.auditorium.domain.entity;

import com.lorafilm.movie.common.audit.BaseAuditableEntity;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.auditorium.domain.enums.ScreenType;
import com.lorafilm.movie.auditorium.domain.enums.SoundType;
import com.lorafilm.movie.auditorium.domain.enums.AuditoriumStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "auditoriums")
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
    private ScreenType screenType = ScreenType.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "sound_type", nullable = false)
    private SoundType soundType = SoundType.STANDARD;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "cleaning_buffer_minutes", nullable = false)
    private Integer cleaningBufferMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditoriumStatus status = AuditoriumStatus.DRAFT;

    public Auditorium() {
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

    public Cinema getCinema() {
        return cinema;
    }

    public void setCinema(Cinema cinema) {
        this.cinema = cinema;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ScreenType getScreenType() {
        return screenType;
    }

    public void setScreenType(ScreenType screenType) {
        this.screenType = screenType;
    }

    public SoundType getSoundType() {
        return soundType;
    }

    public void setSoundType(SoundType soundType) {
        this.soundType = soundType;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getCleaningBufferMinutes() {
        return cleaningBufferMinutes;
    }

    public void setCleaningBufferMinutes(Integer cleaningBufferMinutes) {
        this.cleaningBufferMinutes = cleaningBufferMinutes;
    }

    public AuditoriumStatus getStatus() {
        return status;
    }

    public void setStatus(AuditoriumStatus status) {
        this.status = status;
    }
}
