package com.lorafilm.movie.showtime.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "showtime_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }
}
