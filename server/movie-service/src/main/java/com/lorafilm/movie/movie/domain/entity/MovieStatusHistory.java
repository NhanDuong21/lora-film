package com.lorafilm.movie.movie.domain.entity;

import com.lorafilm.movie.movie.domain.enums.MovieStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movie_status_history", indexes = {
        @Index(name = "idx_movie_status_history_order", columnList = "movie_id, changed_at, id")
})
public class MovieStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private MovieStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private MovieStatus newStatus;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", updatable = false)
    private Long changedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }
    public MovieStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(MovieStatus previousStatus) { this.previousStatus = previousStatus; }
    public MovieStatus getNewStatus() { return newStatus; }
    public void setNewStatus(MovieStatus newStatus) { this.newStatus = newStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
    public Long getChangedBy() { return changedBy; }
    public void setChangedBy(Long changedBy) { this.changedBy = changedBy; }
}
