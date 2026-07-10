package com.lorafilm.movie.showtime.domain.entity;

import com.lorafilm.movie.showtime.domain.enums.ShowtimeStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "showtime_status_history")
public class ShowtimeStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private ShowtimeStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ShowtimeStatus newStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_at", updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", updatable = false)
    private Long changedBy;

    public ShowtimeStatusHistory() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Showtime getShowtime() {
        return showtime;
    }

    public void setShowtime(Showtime showtime) {
        this.showtime = showtime;
    }

    public ShowtimeStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(ShowtimeStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public ShowtimeStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(ShowtimeStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }
}
